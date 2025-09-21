package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
public class PaymentCoreRepository implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    public PaymentCoreRepository(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findByTransactionKey(String transactionKey) {
        return paymentJpaRepository.findById(transactionKey).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findByTransactionKey(String userId, String transactionKey) {
        return paymentJpaRepository.findByUserIdAndTransactionKey(userId, transactionKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByOrderId(String userId, String orderId) {
        List<Payment> list = paymentJpaRepository.findByUserIdAndOrderId(userId, orderId);
        list.sort(Comparator.comparing(Payment::getUpdatedAt).reversed());
        return list;
    }
}

