package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Getter;

@Getter
@Entity
public class Product extends BaseEntity {

    private String name;

    @Embedded
    private Money price;

    @Embedded
    private Stock stock;

    private long likesCount;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    protected Product() {
    }

    public Product(String name, Money price, Stock stock, Brand brand) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);
        validateBrand(brand);

        this.name = name;
        this.price = price;
        this.stock = stock;
        this.brand = brand;
        this.likesCount = 0;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
    }

    private void validatePrice(Money price) {
        if (price == null) {
            throw new IllegalArgumentException("Product price cannot be null");
        }
        if (price.getValue() < 0) {
            throw new IllegalArgumentException("Product price cannot be negative");
        }
    }

    private void validateStock(Stock stock) {
        if (stock == null) {
            throw new IllegalArgumentException("Product stock cannot be null");
        }
        if (stock.getQuantity() < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
    }

    private void validateBrand(Brand brand) {
        if (brand == null) {
            throw new IllegalArgumentException("Product brand cannot be null");
        }
    }

    public void decreaseStock(int quantity) {
        this.stock = this.stock.decrease(quantity);
    }

    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    public Long getBrandId() {
        return this.brand != null ? this.brand.getId() : null;
    }

    public boolean hasEnoughStock(int quantity) {
        return this.stock.getQuantity() >= quantity;
    }

    public Money calculateTotalPrice(int quantity) {
        return this.price.multiply(quantity);
    }

    public Long getId() {
        return super.getId();
    }

    public String getName() {
        return this.name;
    }

    public Money getPrice() {
        return this.price;
    }

    public Stock getStock() {
        return this.stock;
    }

    public long getLikesCount() {
        return this.likesCount;
    }
}
