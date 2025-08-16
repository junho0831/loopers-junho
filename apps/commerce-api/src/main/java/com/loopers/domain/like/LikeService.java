package com.loopers.domain.like;

import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LikeService {
    private final JpaProductLikeRepository productLikeRepository;
    private final JpaProductRepository productRepository;
    private final ProductCacheService cacheService;

    public LikeService(JpaProductLikeRepository productLikeRepository, 
                      JpaProductRepository productRepository, 
                      ProductCacheService cacheService) {
        this.productLikeRepository = productLikeRepository;
        this.productRepository = productRepository;
        this.cacheService = cacheService;
    }

    public ProductLike addLike(String userId, Long productId) {
        validateLikeRequest(userId, productId);
        
        // 1. 기존 좋아요 확인 (멱등성 보장)
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            return existingLike.get();
        }

        // 2. 상품 존재 확인 및 비관적 락 적용 (동시성 제어)
        Product product = loadProductWithLock(productId);

        // 3. 좋아요 생성
        ProductLike newLike = new ProductLike(userId, productId);
        productLikeRepository.save(newLike);

        // 4. 상품 좋아요 수 증가 (비정규화된 필드 업데이트)
        product.incrementLikesCount();
        productRepository.save(product);

        // 5. 관련 캐시 무효화 (데이터 일관성 보장)
        cacheService.evictProductCaches(productId);

        return newLike;
    }

    public void removeLike(String userId, Long productId) {
        validateLikeRequest(userId, productId);
        
        // 1. 기존 좋아요 확인 (멱등성 보장)
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isEmpty()) {
            return;
        }

        // 2. 상품 존재 확인 및 비관적 락 적용 (동시성 제어)
        Product product = loadProductWithLock(productId);

        // 3. 좋아요 삭제
        productLikeRepository.delete(existingLike.get());

        // 4. 상품 좋아요 수 감소 (비정규화된 필드 업데이트)
        product.decrementLikesCount();
        productRepository.save(product);

        // 5. 관련 캐시 무효화 (데이터 일관성 보장)
        cacheService.evictProductCaches(productId);
    }

    public List<ProductLike> getUserLikes(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        return productLikeRepository.findByUserId(userId);
    }

    private Product loadProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    private void validateLikeRequest(String userId, Long productId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
    }
}