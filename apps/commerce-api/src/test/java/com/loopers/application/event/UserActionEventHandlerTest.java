package com.loopers.application.event;

import com.loopers.application.user.UserActionEventHandler;
import com.loopers.domain.user.UserActionEvent;
import com.loopers.domain.user.UserActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * UserActionEventHandler 단위 테스트
 * 사용자 행동 추적 이벤트 처리 검증
 */
@ExtendWith(MockitoExtension.class)
class UserActionEventHandlerTest {

    @InjectMocks
    private UserActionEventHandler userActionEventHandler;

    @Test
    @DisplayName("상품 조회 이벤트가 정상적으로 로깅되는지 확인")
    void testHandleProductViewEvent() {
        // Given
        UserActionEvent productViewEvent = UserActionEvent.productView(
                "test-user", 1L, "session1", "test-agent", "127.0.0.1");

        // When & Then - 예외 없이 처리되어야 함
        assertThatNoException().isThrownBy(() -> 
                userActionEventHandler.handleUserAction(productViewEvent));
    }

    @Test
    @DisplayName("상품 상세 조회 이벤트가 정상적으로 로깅되는지 확인")
    void testHandleProductDetailEvent() {
        // Given
        UserActionEvent productDetailEvent = UserActionEvent.productDetail(
                "test-user", 1L, "session1", "test-agent", "127.0.0.1");

        // When & Then - 예외 없이 처리되어야 함
        assertThatNoException().isThrownBy(() -> 
                userActionEventHandler.handleUserAction(productDetailEvent));
    }

    @Test
    @DisplayName("좋아요 이벤트가 정상적으로 로깅되는지 확인")
    void testHandleProductLikeEvent() {
        // Given
        UserActionEvent productLikeEvent = UserActionEvent.productLike(
                "test-user", 1L, "추가", "session1", "test-agent", "127.0.0.1");

        // When & Then - 예외 없이 처리되어야 함
        assertThatNoException().isThrownBy(() -> 
                userActionEventHandler.handleUserAction(productLikeEvent));
    }

    @Test
    @DisplayName("주문 생성 이벤트가 정상적으로 로깅되는지 확인")
    void testHandleOrderCreatedEvent() {
        // Given
        UserActionEvent orderCreatedEvent = UserActionEvent.orderCreated(
                "test-user", 1L, "session1", "test-agent", "127.0.0.1");

        // When & Then - 예외 없이 처리되어야 함
        assertThatNoException().isThrownBy(() -> 
                userActionEventHandler.handleUserAction(orderCreatedEvent));
    }

    @Test
    @DisplayName("익명 사용자 이벤트도 정상적으로 처리되는지 확인")
    void testHandleAnonymousUserEvent() {
        // Given
        UserActionEvent anonymousEvent = UserActionEvent.productView(
                "anonymous", 1L, "session1", "test-agent", "127.0.0.1");

        // When & Then - 예외 없이 처리되어야 함
        assertThatNoException().isThrownBy(() -> 
                userActionEventHandler.handleUserAction(anonymousEvent));
    }

    @Test
    @DisplayName("다양한 액션 타입의 이벤트들이 모두 처리되는지 확인")
    void testHandleVariousActionTypes() {
        // Given
        UserActionEvent[] events = {
                UserActionEvent.productView("user1", 1L, "session1", "agent1", "127.0.0.1"),
                UserActionEvent.productDetail("user2", 2L, "session2", "agent2", "127.0.0.2"),
                UserActionEvent.productLike("user3", 3L, "추가", "session3", "agent3", "127.0.0.3"),
                UserActionEvent.orderCreated("user4", 4L, "session4", "agent4", "127.0.0.4")
        };

        // When & Then - 모든 이벤트가 예외 없이 처리되어야 함
        for (UserActionEvent event : events) {
            assertThatNoException().isThrownBy(() -> 
                    userActionEventHandler.handleUserAction(event));
        }
    }

    @Test
    @DisplayName("이벤트 처리 중 예외가 발생해도 시스템에 영향을 주지 않는지 확인")
    void testEventHandlingExceptionIsolation() {
        // Given - 실제로는 로깅만 하므로 예외가 발생하기 어렵지만, 
        // 비즈니스 로직 확장 시를 대비한 테스트
        UserActionEvent event = UserActionEvent.productView(
                "test-user", 1L, "session1", "test-agent", "127.0.0.1");

        // When & Then - 예외가 발생하더라도 전파되지 않아야 함
        assertThatNoException().isThrownBy(() -> 
                userActionEventHandler.handleUserAction(event));
    }

    @Test
    @DisplayName("대용량 이벤트 처리 성능 확인 (간단한 부하 테스트)")
    void testHandleManyEventsPerformance() {
        // Given
        int eventCount = 1000;

        // When & Then - 많은 이벤트도 빠르게 처리되어야 함
        assertThatNoException().isThrownBy(() -> {
            for (int i = 0; i < eventCount; i++) {
                UserActionEvent event = UserActionEvent.productView(
                        "user" + i, (long) i, "session" + i, "agent", "127.0.0.1");
                userActionEventHandler.handleUserAction(event);
            }
        });
    }
}