package com.loopers.domain.order;

import java.util.List;

public class OrderRequest {
    private List<OrderItemRequest> items;
    private Long couponId; // 추가된 쿠폰 ID

    public OrderRequest() {}

    public OrderRequest(List<OrderItemRequest> items, Long couponId) {
        this.items = items;
        this.couponId = couponId;
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
}