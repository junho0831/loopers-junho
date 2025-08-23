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

    public Payment createPayment(String orderId, Long userId, BigDecimal amount, String cardType, String cardNo) {
        return new Payment(orderId, userId, amount, cardType, cardNo);
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
