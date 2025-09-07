package com.loopers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Order 이벤트를 위한 타입 안전한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEventDto extends EventDto {
    private Long orderId;
    private String userId;
    private BigDecimal totalAmount;
    private String transactionId;
    private String paymentStatus;

    // 기본 생성자
    public OrderEventDto() {}

    // 필드별 검증 메서드
    public boolean hasOrderId() {
        return orderId != null;
    }
    
    public boolean hasUserId() {
        return userId != null && !userId.trim().isEmpty();
    }
    
    public boolean hasTotalAmount() {
        return totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    // Getter/Setter
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    @Override
    public String toString() {
        return "OrderEventDto{" +
                "eventId='" + getEventId() + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", orderId=" + orderId +
                ", userId='" + userId + '\'' +
                ", totalAmount=" + totalAmount +
                '}';
    }
}