package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentJpaRepository extends JpaRepository<Payment, String> {
    Payment findByUserIdAndTransactionKey(String userId, String transactionKey);
    List<Payment> findByUserIdAndOrderId(String userId, String orderId);
}

