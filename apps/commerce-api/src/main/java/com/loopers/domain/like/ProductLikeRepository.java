package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface ProductLikeRepository {
    Optional<ProductLike> findByUserIdAndProductId(String userId, Long productId);
    List<ProductLike> findByUserId(String userId);
    long countByProductId(Long productId);
}