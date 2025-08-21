package com.loopers.domain.payment;

public interface PaymentGateway {
    PaymentResponse requestPayment(PaymentRequest request);
    PaymentStatusResponse checkPaymentStatus(String transactionId);
    PaymentStatusResponse checkPaymentByOrderId(String orderId);
}