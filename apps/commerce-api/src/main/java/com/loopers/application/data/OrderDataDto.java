package com.loopers.application.data;

import java.math.BigDecimal;

public class OrderDataDto {
    private final Long orderId;
    private final String userId;
    private final BigDecimal totalAmount;
    private final Long timestamp;
    
    public OrderDataDto(Long orderId, String userId, BigDecimal totalAmount, Long timestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
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
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("OrderDataDto{orderId=%d, userId='%s', totalAmount=%s, timestamp=%d}", 
                           orderId, userId, totalAmount, timestamp);
    }
}