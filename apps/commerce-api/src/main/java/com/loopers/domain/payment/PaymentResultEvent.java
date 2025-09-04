package com.loopers.domain.payment;

import java.math.BigDecimal;

public class PaymentResultEvent {
    private final String orderId;
    private final String transactionId;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final String failureReason;
    
    public PaymentResultEvent(String orderId, String transactionId, BigDecimal amount, 
                            PaymentStatus status, String failureReason) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
    }
    
    public static PaymentResultEvent success(String orderId, String transactionId, BigDecimal amount) {
        return new PaymentResultEvent(orderId, transactionId, amount, PaymentStatus.APPROVED, null);
    }
    
    public static PaymentResultEvent failure(String orderId, BigDecimal amount, String failureReason) {
        return new PaymentResultEvent(orderId, null, amount, PaymentStatus.FAILED, failureReason);
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public boolean isSuccess() {
        return PaymentStatus.APPROVED.equals(status);
    }
}
