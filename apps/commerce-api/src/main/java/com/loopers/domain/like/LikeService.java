package com.loopers.domain.like;

import org.springframework.stereotype.Service;

@Service
public class LikeService {

    public ProductLike createLike(String userId, Long productId) {
        validateLikeRequest(userId, productId);
        return new ProductLike(userId, productId);
    }

    public void validateLikeRemoval(String userId, Long productId) {
        validateLikeRequest(userId, productId);
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