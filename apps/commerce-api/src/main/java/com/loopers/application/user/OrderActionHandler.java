package com.loopers.application.user;

import com.loopers.domain.user.UserActionEvent;
import com.loopers.domain.user.UserActionHandler;
import com.loopers.domain.user.UserActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 주문 관련 행동 처리기
 * OCP 준수: 새로운 행동 타입 추가 시 이 클래스는 수정되지 않음
 */
@Component
public class OrderActionHandler implements UserActionHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderActionHandler.class);
    
    @Override
    public boolean supports(UserActionType actionType) {
        return UserActionType.ORDER_CREATED.equals(actionType) || 
               UserActionType.ORDER_CANCELLED.equals(actionType);
    }
    
    @Override
    public void updateStatistics(UserActionEvent event) {
        log.debug("주문 통계 업데이트 - orderId: {}, actionType: {}", 
                 event.getTargetId(), event.getActionType());
        
        if (UserActionType.ORDER_CREATED.equals(event.getActionType())) {
            // orderStatistics.increment(event.getUserId());
        } else if (UserActionType.ORDER_CANCELLED.equals(event.getActionType())) {
            // orderCancelStatistics.increment(event.getUserId());
        }
    }
    
    @Override
    public Object prepareAnalyticsData(UserActionEvent event) {
        // 주문 전용 분석 데이터 구조 (더 상세한 정보 포함)
        return new OrderAnalytics(
            event.getUserId(),
            event.getTargetId(),
            event.getActionType().name(),
            event.getTimestamp(),
            extractOrderValue(event), // 주문 금액 등 추가 정보
            event.getSessionId()
        );
    }
    
    private Long extractOrderValue(UserActionEvent event) {
        // 실제로는 Order 서비스에서 주문 금액을 조회
        return 0L; // 임시값
    }
    
    private record OrderAnalytics(String userId, String orderId, String action, 
                                 java.time.ZonedDateTime timestamp, Long orderValue, String sessionId) {}
}