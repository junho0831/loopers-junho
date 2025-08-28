package com.loopers.domain.order;

import java.math.BigDecimal;

public class OrderCreatedEvent {
    private final Long orderId;
    private final String userId;
    private final BigDecimal totalAmount;
    private final Long couponId;
    
    public OrderCreatedEvent(Long orderId, String userId, BigDecimal totalAmount, Long couponId) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.couponId = couponId;
    }
    
    public static OrderCreatedEvent from(Order order, Long couponId) {
        return new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            BigDecimal.valueOf(order.getTotalAmount().getValue()),
            couponId
        );
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public Long getCouponId() {
        return couponId;
    }
}