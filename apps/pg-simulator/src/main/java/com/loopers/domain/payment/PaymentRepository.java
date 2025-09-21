package com.loopers.domain.payment;

import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);
    Payment findByTransactionKey(String transactionKey);
    Payment findByTransactionKey(String userId, String transactionKey);
    List<Payment> findByOrderId(String userId, String orderId);
}

