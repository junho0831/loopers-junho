package com.loopers.application.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LikeFacade {
    private final JpaProductLikeRepository productLikeRepository;
    private final JpaProductRepository productRepository;
    private final LikeService likeService;

    public LikeFacade(JpaProductLikeRepository productLikeRepository, JpaProductRepository productRepository, LikeService likeService) {
        this.productLikeRepository = productLikeRepository;
        this.productRepository = productRepository;
        this.likeService = likeService;
    }

    public ProductLike addLike(String userId, Long productId) {
        // 1. 기존 좋아요 확인
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            return existingLike.get(); // 멱등성 보장
        }

        // 2. 상품 존재 확인
        Product product = loadProduct(productId);

        // 3. 도메인 서비스를 통한 좋아요 생성
        ProductLike newLike = likeService.createLike(userId, productId);
        productLikeRepository.save(newLike);

        // 4. 상품 좋아요 수 증가 및 저장
        product.incrementLikesCount();
        productRepository.save(product);

        return newLike;
    }

    public void removeLike(String userId, Long productId) {
        // 1. 도메인 서비스를 통한 검증
        likeService.validateLikeRemoval(userId, productId);
        
        // 2. 기존 좋아요 확인
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isEmpty()) {
            return; // 멱등성 보장
        }

        // 3. 상품 존재 확인
        Product product = loadProduct(productId);

        // 4. 좋아요 삭제
        productLikeRepository.delete(existingLike.get());

        // 5. 상품 좋아요 수 감소 및 저장
        product.decrementLikesCount();
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductLike> getLikedProducts(String userId) {
        return productLikeRepository.findByUserId(userId);
    }

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }
}