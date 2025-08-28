package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentStatusResponse;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PENDING 상태의 결제를 주기적으로 확인하여 최종 결제 상태를 업데이트하는 스케줄러
 * 
 * 주요 기능:
 * 1. 5분 이상 PENDING 상태인 결제들을 조회
 * 2. PG에서 실제 결제 상태 확인
 * 3. 결제 상태 및 주문 상태 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final OrderRepository orderRepository;

    /**
     * 매 5분마다 PENDING 결제 복구 작업 실행
     */
    @Scheduled(fixedDelay = 300000) // 5분 (300초)
    @Transactional
    public void recoverPendingPayments() {
        try {
            log.info("=== PENDING 결제 복구 스케줄러 시작 ===");
            
            // 5분 이상 PENDING 상태인 결제들 조회
            List<Payment> pendingPayments = paymentRepository.findPendingPaymentsOlderThan(5);
            
            if (pendingPayments.isEmpty()) {
                log.info("복구 대상 PENDING 결제 없음");
                return;
            }
            
            log.info("복구 대상 PENDING 결제 {}건 발견", pendingPayments.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Payment payment : pendingPayments) {
                try {
                    if (processPaymentRecovery(payment)) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("결제 복구 처리 중 오류 발생 - paymentId: {}, orderId: {}", 
                            payment.getId(), payment.getOrderId(), e);
                    failureCount++;
                }
            }
            
            log.info("=== PENDING 결제 복구 완료 - 성공: {}건, 실패: {}건 ===", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("PENDING 결제 복구 스케줄러 실행 중 오류", e);
        }
    }

    /**
     * 개별 결제 복구 처리
     */
    private boolean processPaymentRecovery(Payment payment) {
        try {
            log.info("결제 복구 처리 시작 - paymentId: {}, orderId: {}", 
                    payment.getId(), payment.getOrderId());
            
            // PG에서 실제 결제 상태 조회
            PaymentStatusResponse statusResponse = paymentGateway.checkPaymentByOrderId(payment.getOrderId());
            
            if (statusResponse.isApproved()) {
                // 결제 승인 처리
                paymentService.approvePayment(payment, statusResponse.getTransactionId());
                paymentRepository.save(payment);
                
                // 주문 상태도 완료로 업데이트
                updateOrderStatus(payment.getOrderId(), "COMPLETED");
                
                log.info("PENDING 결제 승인 완료 - paymentId: {}, orderId: {}", 
                        payment.getId(), payment.getOrderId());
                return true;
                
            } else if (statusResponse.isFailed()) {
                // 결제 실패 처리
                paymentService.failPayment(payment, statusResponse.getMessage());
                paymentRepository.save(payment);
                
                // 주문 상태도 취소로 업데이트
                updateOrderStatus(payment.getOrderId(), "CANCELLED");
                
                log.info("PENDING 결제 실패 확인 - paymentId: {}, orderId: {}, reason: {}", 
                        payment.getId(), payment.getOrderId(), statusResponse.getMessage());
                return true;
                
            } else {
                // 여전히 PENDING 상태 - 다음 스케줄에서 다시 확인
                log.info("PG에서도 여전히 PENDING 상태 - paymentId: {}, orderId: {}", 
                        payment.getId(), payment.getOrderId());
                return true;
            }
            
        } catch (Exception e) {
            log.error("결제 복구 처리 실패 - paymentId: {}, orderId: {}", 
                    payment.getId(), payment.getOrderId(), e);
            
            // PG 조회 실패 시 10분 이상된 결제는 실패로 처리
            if (isPaymentExpired(payment, 10)) {
                paymentService.failPayment(payment, "PG 응답 타임아웃 - 자동 실패 처리");
                paymentRepository.save(payment);
                updateOrderStatus(payment.getOrderId(), "CANCELLED");
                
                log.warn("만료된 PENDING 결제 자동 실패 처리 - paymentId: {}, orderId: {}", 
                        payment.getId(), payment.getOrderId());
                return true;
            }
            
            return false;
        }
    }

    /**
     * 주문 상태 업데이트
     */
    private void updateOrderStatus(String orderId, String status) {
        try {
            orderRepository.findById(Long.parseLong(orderId))
                    .ifPresent(order -> {
                        if ("COMPLETED".equals(status)) {
                            order.complete();
                        } else if ("CANCELLED".equals(status)) {
                            order.cancel();
                        }
                        orderRepository.save(order);
                        log.debug("주문 상태 업데이트 완료 - orderId: {}, status: {}", orderId, status);
                    });
        } catch (Exception e) {
            log.error("주문 상태 업데이트 실패 - orderId: {}, status: {}", orderId, status, e);
        }
    }

    /**
     * 결제가 만료되었는지 확인 (생성 시간 기준)
     */
    private boolean isPaymentExpired(Payment payment, int minutes) {
        return payment.getCreatedAt()
                .isBefore(java.time.ZonedDateTime.now().minusMinutes(minutes));
    }

    /**
     * 수동 복구 실행 메서드 (테스트/운영 목적)
     */
    public void manualRecovery() {
        log.info("수동 PENDING 결제 복구 실행");
        recoverPendingPayments();
    }
}