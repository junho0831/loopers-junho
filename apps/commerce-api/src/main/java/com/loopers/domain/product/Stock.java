package com.loopers.domain.product;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Stock {
    private int quantity;

    protected Stock() {
        // JPA를 위한 기본 생성자
    }

    public Stock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public Stock decrease(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Decrease amount cannot be negative.");
        }
        if (this.quantity < amount) {
            throw new IllegalArgumentException("Insufficient stock.");
        }
        return new Stock(this.quantity - amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return quantity == stock.quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity);
    }

    @Override
    public String toString() {
        return String.valueOf(quantity);
    }
}