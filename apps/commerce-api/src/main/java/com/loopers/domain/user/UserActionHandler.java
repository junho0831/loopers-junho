package com.loopers.domain.user;

/**
 * 사용자 행동 처리 전략 인터페이스
 * OCP 준수: 새로운 행동 타입 추가 시 기존 코드 수정 없이 확장 가능
 */
public interface UserActionHandler {
    
    /**
     * 지원하는 액션 타입인지 확인
     */
    boolean supports(UserActionType actionType);
    
    /**
     * 통계 업데이트 처리
     */
    void updateStatistics(UserActionEvent event);
    
    /**
     * 분석 시스템 전송 데이터 준비
     */
    default Object prepareAnalyticsData(UserActionEvent event) {
        return event; // 기본값: 이벤트 자체
    }
}