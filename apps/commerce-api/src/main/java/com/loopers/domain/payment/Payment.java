package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column
    private String transactionId;

    @Column
    private String cardType;

    @Column
    private String cardNo;

    @Column
    private String errorMessage;

    @Column
    private LocalDateTime completedAt;

    protected Payment() {}

    public Payment(String orderId, Long userId, BigDecimal amount, String cardType, String cardNo) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.cardType = cardType;
        this.cardNo = maskCardNumber(cardNo);
    }

    public void approve(String transactionId) {
        this.status = PaymentStatus.APPROVED;
        this.transactionId = transactionId;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = PaymentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    private String maskCardNumber(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return cardNo;
        }
        return cardNo.substring(0, 4) + "-****-****-" + cardNo.substring(cardNo.length() - 4);
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    // Test helper method
    public void updateTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    // Getters
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
    public String getCardType() { return cardType; }
    public String getCardNo() { return cardNo; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}