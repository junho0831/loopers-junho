package com.loopers.domain.like;

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

    public LikeService(JpaProductLikeRepository productLikeRepository, JpaProductRepository productRepository) {
        this.productLikeRepository = productLikeRepository;
        this.productRepository = productRepository;
    }

    public ProductLike addLike(String userId, Long productId) {
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            // Already liked, idempotent operation
            return existingLike.get();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        ProductLike newLike = new ProductLike(userId, productId);
        productLikeRepository.save(newLike);

        product.incrementLikesCount();
        productRepository.save(product);

        return newLike;
    }

    public void removeLike(String userId, Long productId) {
        Optional<ProductLike> existingLike = productLikeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isEmpty()) {
            // Not liked, idempotent operation
            return;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        productLikeRepository.delete(existingLike.get());

        product.decrementLikesCount();
        productRepository.save(product);
    }

    public List<ProductLike> getLikedProducts(String userId) {
        return productLikeRepository.findByUserId(userId);
    }
}