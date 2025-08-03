package com.loopers.interfaces.api;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.like.ProductLike;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/like")
public class LikeController {

    private final LikeFacade likeFacade;
    private final JpaProductRepository productRepository;

    public LikeController(LikeFacade likeFacade,
            JpaProductRepository productRepository) {
        this.likeFacade = likeFacade;
        this.productRepository = productRepository;
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> addLike(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long productId) {

        // 상품 존재 여부 확인
        if (!productRepository.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        likeFacade.addLike(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long productId) {

        // 상품 존재 여부 확인
        if (!productRepository.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        likeFacade.removeLike(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<LikedProductResponse>>> getLikedProducts(
            @RequestHeader("X-USER-ID") String userId) {

        List<LikedProductResponse> response = likeFacade.getLikedProducts(userId)
                .stream()
                .map(productLike -> {
                    Product product = productRepository.findById(productLike.getProductId())
                            .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
                    return new LikedProductResponse(product);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    public static class LikedProductResponse {
        private final Product product;

        public LikedProductResponse(Product product) {
            this.product = product;
        }

        public Long getProductId() {
            return product.getId();
        }

        public String getName() {
            return product.getName();
        }
    }
}