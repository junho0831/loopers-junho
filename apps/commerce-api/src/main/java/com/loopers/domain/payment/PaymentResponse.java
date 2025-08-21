package com.loopers.domain.payment;

public class PaymentResponse {
    private final String message;
    private final boolean success;
    private final String transactionId;

    public PaymentResponse(String message, boolean success) {
        this(message, success, null);
    }

    public PaymentResponse(String message, boolean success, String transactionId) {
        this.message = message;
        this.success = success;
        this.transactionId = transactionId;
    }

    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
}