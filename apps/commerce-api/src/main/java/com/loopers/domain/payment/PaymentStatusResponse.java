package com.loopers.domain.payment;

public class PaymentStatusResponse {
    private final String transactionId;
    private final String status;
    private final String message;
    private final String orderId;

    public PaymentStatusResponse(String transactionId, String status, String message, String orderId) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.orderId = orderId;
    }

    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getOrderId() { return orderId; }

    public boolean isApproved() {
        return "COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) || "LIMIT_EXCEEDED".equalsIgnoreCase(status) || "INVALID_CARD".equalsIgnoreCase(status);
    }
}