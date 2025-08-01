package com.loopers.application.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.LikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LikeFacade {
    private final LikeService likeService;

    public LikeFacade(LikeService likeService) {
        this.likeService = likeService;
    }

    public ProductLike addLike(String userId, Long productId) {
        return likeService.addLike(userId, productId);
    }

    public void removeLike(String userId, Long productId) {
        likeService.removeLike(userId, productId);
    }

    @Transactional(readOnly = true)
    public List<ProductLike> getLikedProducts(String userId) {
        return likeService.getLikedProducts(userId);
    }
}