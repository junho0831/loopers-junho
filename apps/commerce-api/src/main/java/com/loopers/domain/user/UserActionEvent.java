package com.loopers.domain.user;

import java.time.ZonedDateTime;

public class UserActionEvent {
    private final String userId;
    private final UserActionType actionType;
    private final String targetId;
    private final String details;
    private final ZonedDateTime timestamp;
    private final String sessionId;
    private final String userAgent;
    private final String ipAddress;
    
    public UserActionEvent(String userId, UserActionType actionType, String targetId, 
                          String details, String sessionId, String userAgent, String ipAddress) {
        this.userId = userId;
        this.actionType = actionType;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = ZonedDateTime.now();
        this.sessionId = sessionId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }
    
    // 정적 팩토리 메서드들
    public static UserActionEvent productView(String userId, Long productId, String sessionId, String userAgent, String ipAddress) {
        return new UserActionEvent(userId, UserActionType.PRODUCT_VIEW, productId.toString(), 
                                  "상품 조회", sessionId, userAgent, ipAddress);
    }
    
    public static UserActionEvent productDetail(String userId, Long productId, String sessionId, String userAgent, String ipAddress) {
        return new UserActionEvent(userId, UserActionType.PRODUCT_DETAIL, productId.toString(), 
                                  "상품 상세 조회", sessionId, userAgent, ipAddress);
    }
    
    public static UserActionEvent productLike(String userId, Long productId, String action, String sessionId, String userAgent, String ipAddress) {
        return new UserActionEvent(userId, UserActionType.PRODUCT_LIKE, productId.toString(), 
                                  "좋아요 " + action, sessionId, userAgent, ipAddress);
    }
    
    public static UserActionEvent orderCreated(String userId, Long orderId, String sessionId, String userAgent, String ipAddress) {
        return new UserActionEvent(userId, UserActionType.ORDER_CREATED, orderId.toString(), 
                                  "주문 생성", sessionId, userAgent, ipAddress);
    }

    
    // Getters
    public String getUserId() {
        return userId;
    }
    
    public UserActionType getActionType() {
        return actionType;
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public String getDetails() {
        return details;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    @Override
    public String toString() {
        return String.format("UserActionEvent{userId='%s', actionType=%s, targetId='%s', details='%s', timestamp=%s}", 
                           userId, actionType, targetId, details, timestamp);
    }
}
