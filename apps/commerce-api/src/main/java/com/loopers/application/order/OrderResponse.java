package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponse {
    
    private final Order order;

    public OrderResponse(Order order) {
        this.order = order;
    }

    public Long getOrderId() {
        return order.getId();
    }

    public String getUserId() {
        return order.getUserId();
    }

    public long getTotalAmount() {
            return order.getTotalAmount().getValue();
        }

    public OrderStatus getStatus() {
        return order.getStatus();
    }

    public ZonedDateTime getCreatedAt() {
        return order.getCreatedAt();
    }

    public List<OrderItemResponse> getOrderItems() {
        return order.getOrderItems().stream()
                .map(OrderItemResponse::new)
                .collect(Collectors.toList());
    }

    public static class OrderItemResponse {
        
        private final OrderItem orderItem;

        public OrderItemResponse(OrderItem orderItem) {
            this.orderItem = orderItem;
        }

        public Long getProductId() {
            return orderItem.getProductId();
        }

        public int getQuantity() {
            return orderItem.getQuantity();
        }

        public long getUnitPrice() {
            return orderItem.getUnitPrice().getValue();
        }

        public long getTotalPrice() {
            return orderItem.calculateTotalPrice().getValue();
        }
    }
}