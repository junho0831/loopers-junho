package com.loopers.domain.order;

public class OrderItemRequest {
    private Long productId;
    private int quantity;

    public OrderItemRequest(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}