package com.loopers.interfaces.api;

import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.*;
import com.loopers.infrastructure.payment.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment Callback", description = "결제 콜백 API")
@RestController
@RequestMapping("/api/v1/payments/callback")
public class PaymentCallbackController {
    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentCallbackController(PaymentService paymentService, PaymentRepository paymentRepository, 
                                   OrderRepository orderRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Operation(summary = "결제 콜백 처리", description = "PG에서 호출하는 결제 결과 콜백을 처리합니다")
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> handlePaymentCallback(@RequestBody PaymentCallbackRequest request) {
        try {
            log.info("결제 콜백 수신: transactionId={}, status={}", request.transactionId(), request.status());
            
            PaymentStatusResponse statusResponse = new PaymentStatusResponse(request.transactionId(), request.status(), request.message(), null);
            
            if (statusResponse.isApproved()) {
                paymentRepository.findByTransactionId(request.transactionId())
                        .ifPresent(payment -> {
                            paymentService.approvePayment(payment, request.transactionId());
                            paymentRepository.save(payment);
                            
                            // 직접 주문 상태 업데이트
                            orderRepository.findById(Long.parseLong(payment.getOrderId()))
                                    .ifPresent(order -> {
                                        order.complete();
                                        orderRepository.save(order);
                                        log.info("주문 완료 처리: orderId={}", payment.getOrderId());
                                    });
                        });
            } else if (statusResponse.isFailed()) {
                paymentRepository.findByTransactionId(request.transactionId())
                        .ifPresent(payment -> {
                            paymentService.failPayment(payment, request.message());
                            paymentRepository.save(payment);
                            
                            // 직접 주문 상태 업데이트
                            orderRepository.findById(Long.parseLong(payment.getOrderId()))
                                    .ifPresent(order -> {
                                        order.cancel();
                                        orderRepository.save(order);
                                        log.info("주문 취소 처리: orderId={}", payment.getOrderId());
                                    });
                        });
            }

            return ResponseEntity.ok(ApiResponse.success("콜백 처리 완료"));
        } catch (Exception e) {
            log.error("결제 콜백 처리 중 오류 발생: transactionId={}", request.transactionId(), e);
            return ResponseEntity.ok(ApiResponse.success("콜백 수신 확인")); // PG에게는 성공으로 응답
        }
    }

    public record PaymentCallbackRequest(
            String transactionId,
            String orderId,
            String status,
            String message
    ) {}
}