package com.loopers.domain.payment;

import com.loopers.infrastructure.payment.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(String orderId, String userId, BigDecimal amount, String cardType, String cardNo) {
        // userId를 숫자로 변환할 수 없는 경우 기본값 사용
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("사용자 ID를 숫자로 변환할 수 없음: {}, 기본값 0 사용", userId);
            userIdLong = 0L;
        }
        return new Payment(orderId, userIdLong, amount, cardType, cardNo);
    }

    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public void approvePayment(Payment payment, String transactionId) {
        payment.approve(transactionId);
        paymentRepository.save(payment);
        log.info("결제 승인 처리: orderId={}, transactionId={}", payment.getOrderId(), transactionId);
    }

    public void failPayment(Payment payment, String errorMessage) {
        payment.fail(errorMessage);
        paymentRepository.save(payment);
        log.info("결제 실패 처리: orderId={}, message={}", payment.getOrderId(), errorMessage);
    }
}
