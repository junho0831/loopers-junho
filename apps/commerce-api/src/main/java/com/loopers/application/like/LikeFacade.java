package com.loopers.application.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.ProductLikeEvent;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.UserActionEvent;
import com.loopers.interfaces.api.LikeController.LikedProductResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LikeFacade {
    private final LikeService likeService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    public LikeFacade(LikeService likeService, ProductService productService, ApplicationEventPublisher eventPublisher) {
        this.likeService = likeService;
        this.productService = productService;
        this.eventPublisher = eventPublisher;
    }

    public ProductLike addLike(String userId, Long productId, String sessionId, String userAgent, String ipAddress) {
        System.out.println("DEBUG: LikeFacade.addLike called - userId: " + userId + ", productId: " + productId);
        
        // 상품 존재 여부만 확인하여 영속성 컨텍스트에 미리 로드되지 않도록 한다
        if (!productService.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        ProductLike result = likeService.addLike(userId, productId);
        System.out.println("DEBUG: Like created successfully - result: " + result);
        
        // 도메인 이벤트 발행: 좋아요 집계 및 캐시 업데이트
        ProductLikeEvent event = ProductLikeEvent.liked(userId, productId);
        System.out.println("DEBUG: Publishing ProductLikeEvent - userId: " + userId + ", productId: " + productId + ", event: " + event);
        eventPublisher.publishEvent(event);
        
        // 사용자 행동 추적 이벤트 발행
        publishUserActionEvent(UserActionEvent.productLike(
            userId, productId, "추가", sessionId, userAgent, ipAddress));
        
        return result;
    }

    public void removeLike(String userId, Long productId, String sessionId, String userAgent, String ipAddress) {
        // 상품 존재 여부만 확인하여 영속성 컨텍스트에 미리 로드되지 않도록 한다
        if (!productService.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        likeService.removeLike(userId, productId);
        
        // 도메인 이벤트 발행: 좋아요 집계 및 캐시 업데이트
        eventPublisher.publishEvent(ProductLikeEvent.unliked(userId, productId));
        
        // 사용자 행동 추적 이벤트 발행
        publishUserActionEvent(UserActionEvent.productLike(
            userId, productId, "취소", sessionId, userAgent, ipAddress));
    }

    @Transactional(readOnly = true)
    public List<ProductLike> getLikedProducts(String userId) {
        return likeService.getUserLikes(userId);
    }

    @Transactional(readOnly = true)
    public List<LikedProductResponse> getLikedProductsWithDetails(String userId) {
        List<ProductLike> likes = likeService.getUserLikes(userId);
        return likes.stream()
                .map(productLike -> {
                    Product product = productService.findById(productLike.getProductId())
                            .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
                    return new LikedProductResponse(product.getId(), product.getName());
                })
                .toList();
    }
    
    private void publishUserActionEvent(UserActionEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            // 사용자 행동 추적 실패해도 메인 비즈니스에는 영향 없도록
            // 로그만 남기고 계속 진행
        }
    }
}
