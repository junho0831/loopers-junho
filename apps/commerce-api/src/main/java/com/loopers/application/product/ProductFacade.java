package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSortType;
import com.loopers.interfaces.api.ProductDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductFacade {
    private final ProductService productService;

    public ProductFacade(ProductService productService) {
        this.productService = productService;
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        ProductDetail productDetail = productService.getProductDetail(productId);
        return ProductDetailResponse.from(productDetail);
    }

    public Page<Product> getProducts(ProductSortType sortType, Long brandId, Pageable pageable) {
        return productService.getProducts(sortType, brandId, pageable);
    }

    public Product createProduct(String name, long price, int stock, Long brandId) {
        return productService.createProduct(name, price, stock, brandId);
    }

    public void decreaseProductStock(Long productId, int quantity) {
        productService.decreaseProductStock(productId, quantity);
    }
}