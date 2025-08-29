package com.loopers.domain.order;

import java.util.List;

public class OrderRequest {
    private List<OrderItemRequest> items;
    private Long couponId; // 추가된 쿠폰 ID
    private String cardCompany;
    private String cardNumber;

    public OrderRequest() {}

    public OrderRequest(List<OrderItemRequest> items, Long couponId) {
        this.items = items;
        this.couponId = couponId;
        this.cardCompany = "SAMSUNG"; // 기본값
        this.cardNumber = "1234-5678-9012-3456"; // 기본값
    }

    public OrderRequest(List<OrderItemRequest> items, Long couponId, String cardCompany, String cardNumber) {
        this.items = items;
        this.couponId = couponId;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public boolean hasCoupon() {
        return couponId != null;
    }

    public String getCardCompany() {
        return cardCompany;
    }

    public void setCardCompany(String cardCompany) {
        this.cardCompany = cardCompany;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}