package com.loopers.domain.payment;

public enum PaymentStatus {
    PENDING("결제 대기"),
    APPROVED("결제 승인"),
    FAILED("결제 실패"),
    CANCELLED("결제 취소");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}