package com.loopers.domain.like;

import java.time.ZonedDateTime;

/**
 * 상품 좋아요/취소 이벤트
 * 좋아요 집계 및 캐시 업데이트를 위한 이벤트
 */
public class ProductLikeEvent {
    private final String userId;
    private final Long productId;
    private final LikeAction action;
    private final ZonedDateTime timestamp;
    
    public enum LikeAction {
        LIKED, UNLIKED
    }
    
    private ProductLikeEvent(String userId, Long productId, LikeAction action) {
        this.userId = userId;
        this.productId = productId;
        this.action = action;
        this.timestamp = ZonedDateTime.now();
    }
    
    public static ProductLikeEvent liked(String userId, Long productId) {
        return new ProductLikeEvent(userId, productId, LikeAction.LIKED);
    }
    
    public static ProductLikeEvent unliked(String userId, Long productId) {
        return new ProductLikeEvent(userId, productId, LikeAction.UNLIKED);
    }
    
    // Getters
    public String getUserId() {
        return userId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public LikeAction getAction() {
        return action;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public boolean isLiked() {
        return action == LikeAction.LIKED;
    }
    
    public boolean isUnliked() {
        return action == LikeAction.UNLIKED;
    }
    
    @Override
    public String toString() {
        return String.format("ProductLikeEvent{userId='%s', productId=%d, action=%s, timestamp=%s}", 
                           userId, productId, action, timestamp);
    }
}