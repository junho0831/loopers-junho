package com.loopers.application.event;

import com.loopers.application.payment.PaymentEventHandler;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCreatedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentEventHandler 단위 테스트
 * 결제 이벤트 처리 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerTest {
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private PaymentGateway paymentGateway;
    
    @Mock
    private OrderService orderService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentEventHandler paymentEventHandler;

    private OrderCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new OrderCreatedEvent(1L, "test-user", BigDecimal.valueOf(100000), null, "SAMSUNG", "1234-5678-9012-3456");
    }

    @Test
    @DisplayName("주문 생성 이벤트 처리 시 모든 후속 작업이 실행되는지 확인")
    void testHandleOrderCreatedEvent() {
        // Given
        Payment mockPayment = mock(Payment.class);
        PaymentResponse successResponse = new PaymentResponse("결제 성공", true, "TXN-123");

        when(paymentService.createPayment(anyString(), anyString(), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(mockPayment);
        when(paymentGateway.requestPayment(any(PaymentRequest.class)))
                .thenReturn(successResponse);

        // When
        paymentEventHandler.handlePaymentRequest(testEvent);

        // Then
        // 1. 결제 생성이 호출됨
        verify(paymentService).createPayment(eq("1"), eq("test-user"), eq(BigDecimal.valueOf(100000)), eq("SAMSUNG"), eq("1234-5678-9012-3456"));
        
        // 2. PG 결제 요청이 호출됨
        verify(paymentGateway).requestPayment(any(PaymentRequest.class));
        
        // 3. 결제 성공 이벤트 발행
        verify(eventPublisher).publishEvent(any(PaymentResultEvent.class));
    }


    @Test
    @DisplayName("PG 결제 실패 시 실패 이벤트가 발행되는지 확인")
    void testPaymentFailureEventPublished() {
        // Given
        Payment mockPayment = mock(Payment.class);
        PaymentResponse failureResponse = new PaymentResponse("결제 실패", false);

        when(paymentService.createPayment(anyString(), anyString(), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(mockPayment);
        when(paymentGateway.requestPayment(any(PaymentRequest.class)))
                .thenReturn(failureResponse);

        // When
        paymentEventHandler.handlePaymentRequest(testEvent);

        // Then
        verify(eventPublisher).publishEvent(argThat((Object event) -> 
            event instanceof PaymentResultEvent && 
            !((PaymentResultEvent) event).isSuccess()));
    }

    @Test
    @DisplayName("결제 결과 이벤트 처리 - 성공 케이스")
    void testHandlePaymentResultSuccess() {
        // Given
        PaymentResultEvent successEvent = PaymentResultEvent.success("1", "TXN-123", BigDecimal.valueOf(100000));
        Order mockOrder = mock(Order.class);
        
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(mockOrder));

        // When
        paymentEventHandler.handlePaymentResult(successEvent);

        // Then
        verify(mockOrder).completePayment();
        verify(orderService).saveOrder(mockOrder);
    }

    @Test
    @DisplayName("결제 결과 이벤트 처리 - 실패 케이스")
    void testHandlePaymentResultFailure() {
        // Given
        PaymentResultEvent failureEvent = PaymentResultEvent.failure("1", BigDecimal.valueOf(100000), "카드 한도 초과");
        Order mockOrder = mock(Order.class);
        
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(mockOrder));

        // When
        paymentEventHandler.handlePaymentResult(failureEvent);

        // Then
        verify(mockOrder).failPayment();
        verify(orderService).saveOrder(mockOrder);
    }

    @Test
    @DisplayName("PG 요청 예외 발생 시 실패 이벤트 발행")
    void testPaymentGatewayExceptionHandling() {
        // Given
        Payment mockPayment = mock(Payment.class);
        
        when(paymentService.createPayment(anyString(), anyString(), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(mockPayment);
        when(paymentGateway.requestPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("PG 서버 오류"));

        // When
        paymentEventHandler.handlePaymentRequest(testEvent);

        // Then
        verify(eventPublisher).publishEvent(argThat((Object event) ->
            event instanceof PaymentResultEvent &&
            !((PaymentResultEvent) event).isSuccess()));
    }

}