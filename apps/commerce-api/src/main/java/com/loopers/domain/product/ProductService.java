package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private final JpaProductRepository productRepository;

    public ProductService(JpaProductRepository productRepository) {
        this.productRepository = productRepository;
    }

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

    public List<Product> loadProductsWithLock(List<OrderItemRequest> itemRequests) {
        List<Product> products = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findByIdWithLock(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));
            products.add(product);
        }
        return products;
    }

    public void decreaseStock(List<Product> products, List<OrderItemRequest> itemRequests) {
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest itemRequest = itemRequests.get(i);
            product.decreaseStock(itemRequest.getQuantity());
        }
        productRepository.saveAll(products);
    }
}