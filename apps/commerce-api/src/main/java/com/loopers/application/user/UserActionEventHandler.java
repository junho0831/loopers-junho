package com.loopers.application.user;

import com.loopers.domain.user.UserActionEvent;
import com.loopers.domain.user.UserActionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserActionEventHandler {
    private static final Logger log = LoggerFactory.getLogger(UserActionEventHandler.class);
    
    private final List<UserActionHandler> actionHandlers;
    
    public UserActionEventHandler(List<UserActionHandler> actionHandlers) {
        this.actionHandlers = actionHandlers;
    }
    
    @EventListener
    @Async
    public void handleUserAction(UserActionEvent event) {
        try {
            // 1. 사용자 행동 로깅 (구조화된 로그)
            logUserAction(event);
            
            // 2. 사용자 행동 통계 업데이트 (실제로는 별도 DB나 Redis에 저장)
            updateUserActionStatistics(event);
            
            // 3. 실시간 분석을 위한 데이터 전송 (실제로는 Kafka, ElasticSearch 등)
            sendToAnalyticsSystem(event);
            
        } catch (Exception e) {
            // 사용자 행동 추적 실패해도 메인 비즈니스에는 영향 없도록
            log.error("사용자 행동 추적 실패 - userId: {}, actionType: {}", 
                     event.getUserId(), event.getActionType(), e);
        }
    }
    
    private void logUserAction(UserActionEvent event) {
        // 구조화된 로깅 (JSON 형태로 로깅하면 ELK Stack에서 분석하기 좋음)
        log.info("USER_ACTION: userId={}, actionType={}, targetId={}, details={}, " +
                "timestamp={}, sessionId={}, userAgent={}, ipAddress={}", 
                event.getUserId(), event.getActionType(), event.getTargetId(), 
                event.getDetails(), event.getTimestamp(), event.getSessionId(),
                maskUserAgent(event.getUserAgent()), maskIpAddress(event.getIpAddress()));
    }
    
    private void updateUserActionStatistics(UserActionEvent event) {
        // OCP 준수: Strategy Pattern을 통해 새로운 액션 타입 추가 시 기존 코드 수정 없음
        actionHandlers.stream()
            .filter(handler -> handler.supports(event.getActionType()))
            .forEach(handler -> {
                try {
                    handler.updateStatistics(event);
                } catch (Exception e) {
                    log.warn("통계 업데이트 실패 - actionType: {}, handler: {}", 
                            event.getActionType(), handler.getClass().getSimpleName(), e);
                }
            });
    }
    
    private void sendToAnalyticsSystem(UserActionEvent event) {
        // OCP 준수: 각 핸들러가 자신만의 분석 데이터 구조를 정의
        actionHandlers.stream()
            .filter(handler -> handler.supports(event.getActionType()))
            .forEach(handler -> {
                try {
                    Object analyticsData = handler.prepareAnalyticsData(event);
                    
                    // 실제로는 Kafka, Google Analytics 등으로 전송
                    log.debug("분석 시스템으로 사용자 행동 데이터 전송 - userId: {}, actionType: {}, data: {}", 
                             event.getUserId(), event.getActionType(), analyticsData);
                    
                    // 간혹 실패를 시뮬레이션
                    if (Math.random() < 0.05) { // 5% 확률로 실패
                        throw new RuntimeException("분석 시스템 일시적 장애");
                    }
                    
                } catch (Exception e) {
                    log.warn("분석 시스템 전송 실패 - userId: {}, actionType: {}, handler: {}", 
                            event.getUserId(), event.getActionType(), handler.getClass().getSimpleName(), e);
                }
            });
    }
    
    // 개인정보 보호를 위한 마스킹
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() < 10) {
            return "***";
        }
        return userAgent.substring(0, 10) + "***";
    }
    
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "***";
        }
        // IPv4 주소의 마지막 옥텟 마스킹 (예: 192.168.1.*** )
        int lastDot = ipAddress.lastIndexOf('.');
        if (lastDot > 0) {
            return ipAddress.substring(0, lastDot) + ".***";
        }
        return "***";
    }
}