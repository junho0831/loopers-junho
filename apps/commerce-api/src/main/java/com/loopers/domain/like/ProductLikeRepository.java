package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

/**
 * 도메인 레이어의 ProductLike Repository 인터페이스
 * DIP(Dependency Inversion Principle)를 준수하여 도메인이 인프라스트럭처에 의존하지 않도록 함
 */
public interface ProductLikeRepository {
    
    ProductLike save(ProductLike productLike);
    
    void delete(ProductLike productLike);
    
    Optional<ProductLike> findByUserIdAndProductId(String userId, Long productId);
    
    List<ProductLike> findByUserId(String userId);
    
    List<ProductLike> findByProductId(Long productId);
}