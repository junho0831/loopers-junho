package com.loopers.domain.like;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LikeService {
    private final ProductLikeRepository productLikeRepository;

    public LikeService(ProductLikeRepository productLikeRepository) {
        this.productLikeRepository = productLikeRepository;
    }

    public ProductLike addLike(String userId, Long productId) {
        validateLikeRequest(userId, productId);
        
        // 기존 좋아요 확인 (멱등성 보장)
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            return existingLike.get();
        }

        // 좋아요 생성 (순수한 도메인 로직)
        ProductLike newLike = new ProductLike(userId, productId);
        return productLikeRepository.save(newLike);
    }

    public void removeLike(String userId, Long productId) {
        validateLikeRequest(userId, productId);
        
        // 기존 좋아요 확인 (멱등성 보장)
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isEmpty()) {
            return;
        }

        // 좋아요 삭제 (순수한 도메인 로직)
        productLikeRepository.delete(existingLike.get());
    }

    public List<ProductLike> getUserLikes(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        return productLikeRepository.findByUserId(userId);
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