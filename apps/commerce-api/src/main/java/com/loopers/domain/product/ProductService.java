package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    public ProductDetail createProductDetail(Product product, Brand brand) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (brand == null) {
            throw new IllegalArgumentException("Brand cannot be null");
        }
        
        return new ProductDetail(product, brand);
    }

    public void validateProductCreation(String name, long price, int stock) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
    }
}