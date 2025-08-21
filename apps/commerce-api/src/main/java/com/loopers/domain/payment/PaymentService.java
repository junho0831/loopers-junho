package com.loopers.domain.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService() {
    }

    public Payment createPayment(String orderId, Long userId, BigDecimal amount, String cardType, String cardNo) {
        return new Payment(orderId, userId, amount, cardType, cardNo);
    }

    public void approvePayment(Payment payment, String transactionId) {
        payment.approve(transactionId);
        log.info("결제 승인 처리: orderId={}, transactionId={}", payment.getOrderId(), transactionId);
    }

    public void failPayment(Payment payment, String errorMessage) {
        payment.fail(errorMessage);
        log.info("결제 실패 처리: orderId={}, message={}", payment.getOrderId(), errorMessage);
    }
}
