package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Money;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "order_item")
public class OrderItem extends BaseEntity {
    private Long productId;
    private int quantity;

    @Embedded
    private Money priceAtOrder;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    protected OrderItem() {
    }

    public OrderItem(Long productId, int quantity, Money priceAtOrder) {
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return priceAtOrder;
    }

    public Money calculateTotalPrice() {
        return priceAtOrder.multiply(quantity);
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}