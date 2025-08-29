package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCreatedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Optional;

@Component
public class PaymentEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);
    
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;
    
    public PaymentEventHandler(PaymentService paymentService, PaymentGateway paymentGateway,
                               OrderService orderService, ApplicationEventPublisher eventPublisher) {
        this.paymentService = paymentService;
        this.paymentGateway = paymentGateway;
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentRequest(OrderCreatedEvent event) {
        log.info("결제 요청 처리 시작 - orderId: {}", event.getOrderId());
        
        try {
            // 결제 엔티티 생성
            Payment payment = paymentService.createPayment(
                event.getOrderId().toString(),
                event.getUserId(),
                event.getTotalAmount(),
                event.getCardCompany(),
                event.getCardNumber()
            );
            paymentService.savePayment(payment);
            
            // PG 결제 요청
            PaymentRequest paymentRequest = new PaymentRequest(
                event.getOrderId().toString(),
                event.getCardCompany(),
                event.getCardNumber(),
                event.getTotalAmount(),
                "http://localhost:8080/api/v1/payments/callback"
            );
            
            PaymentResponse pgResponse = paymentGateway.requestPayment(paymentRequest);
            
            if (pgResponse.isSuccess() && pgResponse.getTransactionId() != null) {
                payment.updateTransactionId(pgResponse.getTransactionId());
                paymentService.savePayment(payment);
                
                // 결제 성공 이벤트 발행
                eventPublisher.publishEvent(PaymentResultEvent.success(
                    event.getOrderId().toString(),
                    pgResponse.getTransactionId(),
                    event.getTotalAmount()
                ));
                
                log.info("결제 요청 성공 - orderId: {}, transactionId: {}", 
                        event.getOrderId(), pgResponse.getTransactionId());
            } else {
                // 결제 실패 이벤트 발행
                eventPublisher.publishEvent(PaymentResultEvent.failure(
                    event.getOrderId().toString(),
                    event.getTotalAmount(),
                    "PG 결제 실패"
                ));
                
                log.warn("결제 요청 실패 - orderId: {}", event.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("결제 요청 처리 실패 - orderId: {}", event.getOrderId(), e);
            
            // 결제 실패 이벤트 발행
            eventPublisher.publishEvent(PaymentResultEvent.failure(
                event.getOrderId().toString(),
                event.getTotalAmount(),
                e.getMessage()
            ));
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentResult(PaymentResultEvent event) {
        log.info("결제 결과 이벤트 처리 - orderId: {}, status: {}", event.getOrderId(), event.getStatus());
        
        try {
            Optional<Order> orderOpt = orderService.getOrderById(Long.parseLong(event.getOrderId()));
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                
                if (event.isSuccess()) {
                    order.completePayment();
                    log.info("주문 결제 완료 처리 - orderId: {}", event.getOrderId());
                } else {
                    order.failPayment();
                    log.warn("주문 결제 실패 처리 - orderId: {}, reason: {}", 
                            event.getOrderId(), event.getFailureReason());
                }
                
                orderService.saveOrder(order);
            }
        } catch (Exception e) {
            log.error("결제 결과 처리 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
}