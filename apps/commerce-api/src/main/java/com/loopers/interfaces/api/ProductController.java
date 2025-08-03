package com.loopers.interfaces.api;

import com.loopers.application.product.ProductFacade;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.product.ProductSortType;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductFacade productFacade;

    public ProductController(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSortType sortType;
        try {
            sortType = ProductSortType.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_SORT_TYPE);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productFacade.getProducts(sortType, brandId, pageable);
        
        // null 체크 추가
        if (products == null) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }
        
        Page<ProductListResponse> response = products.map(ProductListResponse::new);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long productId) {
        ProductDetailResponse productDetail = productFacade.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(productDetail));
    }

    public static class ProductListResponse {
        private final Product product;

        public ProductListResponse(Product product) {
            this.product = product;
        }

        public Long getProductId() {
            return product.getId();
        }

        public String getName() {
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
            return product.getBrandId();
        }
    }
}