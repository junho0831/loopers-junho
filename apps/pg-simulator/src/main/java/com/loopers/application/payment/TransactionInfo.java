package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.TransactionStatus;

public class TransactionInfo {
    private final String transactionKey;
    private final String orderId;
    private final CardType cardType;
    private final String cardNo;
    private final long amount;
    private final TransactionStatus status;
    private final String reason;

    public TransactionInfo(String transactionKey, String orderId, CardType cardType, String cardNo, long amount,
                           TransactionStatus status, String reason) {
        this.transactionKey = transactionKey;
        this.orderId = orderId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
    }

    public static TransactionInfo from(Payment payment) {
        return new TransactionInfo(
                payment.getTransactionKey(),
                payment.getOrderId(),
                payment.getCardType(),
                payment.getCardNo(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getReason()
        );
    }

    public String getTransactionKey() { return transactionKey; }
    public String getOrderId() { return orderId; }
    public CardType getCardType() { return cardType; }
    public String getCardNo() { return cardNo; }
    public long getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getReason() { return reason; }
}

