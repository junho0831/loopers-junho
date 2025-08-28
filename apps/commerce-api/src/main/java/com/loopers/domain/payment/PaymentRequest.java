package com.loopers.domain.payment;

import java.math.BigDecimal;

public class PaymentRequest {
    private final String orderId;
    private final String cardType;
    private final String cardNo;
    private final BigDecimal amount;
    private final String callbackUrl;

    public PaymentRequest(String orderId, String cardType, String cardNo, BigDecimal amount, String callbackUrl) {
        this.orderId = orderId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.callbackUrl = callbackUrl;
    }

    public String getOrderId() { return orderId; }
    public String getCardType() { return cardType; }
    public String getCardNo() { return cardNo; }
    public BigDecimal getAmount() { return amount; }
    public String getCallbackUrl() { return callbackUrl; }
}