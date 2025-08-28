package com.loopers.application.user;

import com.loopers.domain.user.UserActionEvent;
import com.loopers.domain.user.UserActionHandler;
import com.loopers.domain.user.UserActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 상품 좋아요 행동 처리기
 * OCP 준수: 새로운 행동 타입 추가 시 이 클래스는 수정되지 않음
 */
@Component
public class ProductLikeActionHandler implements UserActionHandler {
    private static final Logger log = LoggerFactory.getLogger(ProductLikeActionHandler.class);
    
    @Override
    public boolean supports(UserActionType actionType) {
        return UserActionType.PRODUCT_LIKE.equals(actionType) || 
               UserActionType.PRODUCT_UNLIKE.equals(actionType);
    }
    
    @Override
    public void updateStatistics(UserActionEvent event) {
        log.debug("상품 좋아요 통계 업데이트 - productId: {}, actionType: {}", 
                 event.getTargetId(), event.getActionType());
        
        if (UserActionType.PRODUCT_LIKE.equals(event.getActionType())) {
            // productLikeStatistics.increment(event.getTargetId());
        } else if (UserActionType.PRODUCT_UNLIKE.equals(event.getActionType())) {
            // productLikeStatistics.decrement(event.getTargetId());
        }
    }
    
    @Override
    public Object prepareAnalyticsData(UserActionEvent event) {
        // 좋아요 전용 분석 데이터 구조
        return new ProductLikeAnalytics(
            event.getUserId(),
            event.getTargetId(),
            event.getActionType().name(),
            event.getTimestamp(),
            extractProductCategory(event), // 상품 카테고리 등 추가 정보
            event.getSessionId()
        );
    }
    
    private String extractProductCategory(UserActionEvent event) {
        // 실제로는 Product 서비스에서 상품 카테고리를 조회
        return "ELECTRONICS"; // 임시값
    }
    
    private record ProductLikeAnalytics(String userId, String productId, String action, 
                                       java.time.ZonedDateTime timestamp, String productCategory, String sessionId) {}
}