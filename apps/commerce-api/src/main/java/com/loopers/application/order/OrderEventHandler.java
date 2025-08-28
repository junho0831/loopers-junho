package com.loopers.application.order;

import com.loopers.application.data.DataPlatformService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCreatedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Optional;

@Component
public class OrderEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;
    private final DataPlatformService dataPlatformService;
    
    public OrderEventHandler(CouponService couponService, PaymentService paymentService,
                           PaymentGateway paymentGateway, OrderService orderService,
                           ApplicationEventPublisher eventPublisher, DataPlatformService dataPlatformService) {
        this.couponService = couponService;
        this.paymentService = paymentService;
        this.paymentGateway = paymentGateway;
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
        this.dataPlatformService = dataPlatformService;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리 시작 - orderId: {}", event.getOrderId());
        
        try {
            // 1. 쿠폰 사용 처리 (별도 트랜잭션)
            if (event.getCouponId() != null) {
                handleCouponUsage(event);
            }
            
            // 2. 결제 요청 처리 (별도 트랜잭션)
            handlePaymentRequest(event);
            
            // 3. 데이터 플랫폼 전송 (비동기)
            handleDataPlatformTransfer(event);
            
        } catch (Exception e) {
            log.error("주문 생성 이벤트 처리 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponUsage(OrderCreatedEvent event) {
        try {
            UserCoupon userCoupon = couponService.findUserCouponById(event.getCouponId());
            if (userCoupon != null && !userCoupon.isUsed()) {
                couponService.useCoupon(userCoupon, event.getOrderId());
                log.info("쿠폰 사용 완료 - couponId: {}, orderId: {}", event.getCouponId(), event.getOrderId());
            }
        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패 - couponId: {}, orderId: {}", 
                     event.getCouponId(), event.getOrderId(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentRequest(OrderCreatedEvent event) {
        try {
            // 결제 엔티티 생성
            Payment payment = paymentService.createPayment(
                event.getOrderId().toString(),
                event.getUserId(),
                event.getTotalAmount(),
                "SAMSUNG", // 기본값, 실제로는 요청에서 받아야 함
                "1234-5678-9012-3456" // 기본값, 실제로는 요청에서 받아야 함
            );
            paymentService.savePayment(payment);
            
            // PG 결제 요청
            PaymentRequest paymentRequest = new PaymentRequest(
                event.getOrderId().toString(),
                "SAMSUNG",
                "1234-5678-9012-3456",
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
    
    @Async
    public void handleDataPlatformTransfer(OrderCreatedEvent event) {
        try {
            dataPlatformService.sendOrderData(event);
            log.info("데이터 플랫폼 전송 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
    
    @EventListener
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
