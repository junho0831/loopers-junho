package com.loopers.interfaces.api;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.brand.Brand;

public class ProductDetailResponse {
    private final Product product;
    private final Brand brand;

    public ProductDetailResponse(Product product, Brand brand) {
        this.product = product;
        this.brand = brand;
    }

    public static ProductDetailResponse from(ProductDetail productDetail) {
        return new ProductDetailResponse(productDetail.getProduct(), productDetail.getBrand());
    }

    public Long getProductId() {
        return product.getId();
    }

    public String getProductName() {
        return product.getName();
    }

    public long getPrice() {
        return product.getPrice().getValue();
    }

    public int getStockQuantity() {
        return product.getStock().getQuantity();
    }

    public long getLikeCount() {
        return product.getLikesCount();
    }

    public Long getBrandId() {
        return brand.getId();
    }

    public String getBrandName() {
        return brand.getName();
    }
}