package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_user_transaction", columnList = "user_id, transaction_key"),
                @Index(name = "idx_user_order", columnList = "user_id, order_id"),
                @Index(name = "idx_unique_user_order_transaction", columnList = "user_id, order_id, transaction_key", unique = true)
        })
public class Payment {

    @Id
    @Column(name = "transaction_key", nullable = false, unique = true)
    private String transactionKey;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "card_no", nullable = false)
    private String cardNo;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "callback_url", nullable = false)
    private String callbackUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Payment() {}

    public Payment(String transactionKey, String userId, String orderId, CardType cardType, String cardNo, long amount, String callbackUrl) {
        this.transactionKey = transactionKey;
        this.userId = userId;
        this.orderId = orderId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.callbackUrl = callbackUrl;
    }

    public void approve() {
        if (status != TransactionStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제승인은 대기상태에서만 가능합니다.");
        }
        status = TransactionStatus.SUCCESS;
        reason = "정상 승인되었습니다.";
        updatedAt = LocalDateTime.now();
    }

    public void invalidCard() {
        if (status != TransactionStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제처리는 대기상태에서만 가능합니다.");
        }
        status = TransactionStatus.FAILED;
        reason = "잘못된 카드입니다. 다른 카드를 선택해주세요.";
        updatedAt = LocalDateTime.now();
    }

    public void limitExceeded() {
        if (status != TransactionStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "한도초과 처리는 대기상태에서만 가능합니다.");
        }
        status = TransactionStatus.FAILED;
        reason = "한도초과입니다. 다른 카드를 선택해주세요.";
        updatedAt = LocalDateTime.now();
    }

    public String getTransactionKey() { return transactionKey; }
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public CardType getCardType() { return cardType; }
    public String getCardNo() { return cardNo; }
    public long getAmount() { return amount; }
    public String getCallbackUrl() { return callbackUrl; }
    public TransactionStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

