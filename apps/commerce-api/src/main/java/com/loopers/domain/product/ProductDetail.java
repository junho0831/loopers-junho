package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;

public class ProductDetail {
    private final Product product;
    private final Brand brand;

    public ProductDetail(Product product, Brand brand) {
        this.product = product;
        this.brand = brand;
    }

    public Product getProduct() {
        return product;
    }

    public Brand getBrand() {
        return brand;
    }
}