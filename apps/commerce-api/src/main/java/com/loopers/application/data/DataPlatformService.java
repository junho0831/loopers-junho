package com.loopers.application.data;

import com.loopers.domain.order.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataPlatformService {
    private static final Logger log = LoggerFactory.getLogger(DataPlatformService.class);
    
    public void sendOrderData(OrderCreatedEvent event) {
        try {
            // 실제로는 외부 데이터 플랫폼(예: Kafka, HTTP API 등)으로 데이터 전송
            // 여기서는 로깅으로 대체
            
            OrderDataDto orderData = createOrderDataDto(event);
            
            // 외부 시스템으로 전송하는 로직 (예: REST API 호출, 메시지 큐 전송 등)
            sendToExternalSystem(orderData);
            
            log.info("주문 데이터 플랫폼 전송 성공 - orderId: {}, userId: {}, amount: {}", 
                    event.getOrderId(), event.getUserId(), event.getTotalAmount());
                    
        } catch (Exception e) {
            log.error("주문 데이터 플랫폼 전송 실패 - orderId: {}", event.getOrderId(), e);
            throw new DataPlatformException("데이터 플랫폼 전송 실패", e);
        }
    }
    
    private OrderDataDto createOrderDataDto(OrderCreatedEvent event) {
        return new OrderDataDto(
            event.getOrderId(),
            event.getUserId(),
            event.getTotalAmount(),
            System.currentTimeMillis() // timestamp
        );
    }
    
    private void sendToExternalSystem(OrderDataDto orderData) {
        // 실제 구현에서는 여기에서:
        // 1. HTTP 클라이언트로 데이터 플랫폼 API 호출
        // 2. Kafka Producer로 메시지 전송
        // 3. AWS SQS, RabbitMQ 등 메시지 큐 전송
        // 등의 작업을 수행
        
        // 현재는 로깅으로 대체
        log.debug("외부 데이터 플랫폼으로 전송: {}", orderData);
        
        // 간혹 실패를 시뮬레이션하기 위한 코드 (실제 구현에서는 제거)
        if (Math.random() < 0.1) { // 10% 확률로 실패
            throw new RuntimeException("데이터 플랫폼 일시적 장애");
        }
    }
    
    public static class DataPlatformException extends RuntimeException {
        public DataPlatformException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}