package com.loopers.domain.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 주문 관련 변경사항에 대한 도메인 이벤트
 */
public class OrderEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private String userId;
    private BigDecimal totalAmount;
    private Long couponId;
    private String cardCompany;
    private String cardNumber;
    private List<OrderItemData> items;
    private String transactionId;
    private String paymentStatus;
    private String failureReason;
    private Long timestamp;
    private Long version;

    // 빌더 패턴을 위한 private 생성자
    private OrderEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.version = System.currentTimeMillis();
    }

    public static OrderEvent orderCreated(Long orderId, String userId, BigDecimal totalAmount, 
                                        Long couponId, String cardCompany, String cardNumber,
                                        List<OrderItemData> items) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("totalAmount is required");
        }
        
        OrderEvent event = new OrderEvent();
        event.eventType = "ORDER_CREATED";
        event.orderId = orderId;
        event.userId = userId;
        event.totalAmount = totalAmount;
        event.couponId = couponId;
        event.cardCompany = cardCompany;
        event.cardNumber = cardNumber;
        event.items = items != null ? items : List.of();
        return event;
    }

    public static OrderEvent paymentProcessed(Long orderId, String transactionId, BigDecimal amount,
                                            String paymentStatus, String failureReason) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (transactionId == null) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        
        OrderEvent event = new OrderEvent();
        event.eventType = "PAYMENT_PROCESSED";
        event.orderId = orderId;
        event.transactionId = transactionId;
        event.totalAmount = amount;
        event.paymentStatus = paymentStatus;
        event.failureReason = failureReason;
        return event;
    }

    // 주문 항목을 위한 내부 클래스
    public static class OrderItemData {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;

        public OrderItemData(Long productId, Integer quantity, BigDecimal price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }

        // Getter 메서드들
        public Long getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getPrice() { return price; }
    }

    // Getter 메서드들
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Long getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Long getCouponId() { return couponId; }
    public String getCardCompany() { return cardCompany; }
    public String getCardNumber() { return cardNumber; }
    public List<OrderItemData> getItems() { return items; }
    public String getTransactionId() { return transactionId; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getFailureReason() { return failureReason; }
    public Long getTimestamp() { return timestamp; }
    public Long getVersion() { return version; }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", orderId=" + orderId +
                ", userId='" + userId + '\'' +
                ", totalAmount=" + totalAmount +
                ", timestamp=" + timestamp +
                ", version=" + version +
                '}';
    }
}