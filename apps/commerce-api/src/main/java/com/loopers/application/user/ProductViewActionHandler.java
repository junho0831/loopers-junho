package com.loopers.application.user;

import com.loopers.domain.user.UserActionEvent;
import com.loopers.domain.user.UserActionHandler;
import com.loopers.domain.user.UserActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 상품 조회 행동 처리기
 * OCP 준수: 새로운 행동 타입 추가 시 이 클래스는 수정되지 않음
 */
@Component
public class ProductViewActionHandler implements UserActionHandler {
    private static final Logger log = LoggerFactory.getLogger(ProductViewActionHandler.class);
    
    @Override
    public boolean supports(UserActionType actionType) {
        return UserActionType.PRODUCT_VIEW.equals(actionType) || 
               UserActionType.PRODUCT_DETAIL.equals(actionType);
    }
    
    @Override
    public void updateStatistics(UserActionEvent event) {
        log.debug("상품 조회 통계 업데이트 - productId: {}, actionType: {}", 
                 event.getTargetId(), event.getActionType());
        
        // 실제 구현에서는 Redis나 별도 DB에 저장
        // productViewStatistics.increment(event.getTargetId());
        
        if (UserActionType.PRODUCT_DETAIL.equals(event.getActionType())) {
            // 상품 상세 조회는 별도 처리
            // productDetailStatistics.increment(event.getTargetId());
        }
    }
    
    @Override
    public Object prepareAnalyticsData(UserActionEvent event) {
        // 상품 조회 전용 분석 데이터 구조
        return new ProductViewAnalytics(
            event.getUserId(),
            event.getTargetId(),
            event.getActionType().name(),
            event.getTimestamp()
        );
    }
    
    private record ProductViewAnalytics(String userId, String productId, String action, java.time.ZonedDateTime timestamp) {}
}