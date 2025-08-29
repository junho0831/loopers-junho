package com.loopers.domain.order;

import java.math.BigDecimal;

public class OrderCreatedEvent {
    private final Long orderId;
    private final String userId;
    private final BigDecimal totalAmount;
    private final Long couponId;
    private final String cardCompany;
    private final String cardNumber;
    
    public OrderCreatedEvent(Long orderId, String userId, BigDecimal totalAmount, Long couponId, 
                           String cardCompany, String cardNumber) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.couponId = couponId;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
    }
    
    public static OrderCreatedEvent from(Order order, Long couponId, String cardCompany, String cardNumber) {
        return new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            BigDecimal.valueOf(order.getTotalAmount().getValue()),
            couponId,
            cardCompany,
            cardNumber
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
    
    public String getCardCompany() {
        return cardCompany;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
}