package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :threshold")
    List<Payment> findPendingPaymentsOlderThan(@Param("threshold") LocalDateTime threshold);
    
    // 기존 메서드 호환을 위한 default 메서드
    default List<Payment> findPendingPaymentsOlderThan(int minutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
        return findPendingPaymentsOlderThan(threshold);
    }
}