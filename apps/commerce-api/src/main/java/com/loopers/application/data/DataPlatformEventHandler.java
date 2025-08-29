package com.loopers.application.data;

import com.loopers.domain.order.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class DataPlatformEventHandler {
    private static final Logger log = LoggerFactory.getLogger(DataPlatformEventHandler.class);
    
    private final DataPlatformService dataPlatformService;
    
    public DataPlatformEventHandler(DataPlatformService dataPlatformService) {
        this.dataPlatformService = dataPlatformService;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleDataPlatformTransfer(OrderCreatedEvent event) {
        log.info("데이터 플랫폼 전송 처리 시작 - orderId: {}", event.getOrderId());
        
        try {
            dataPlatformService.sendOrderData(event);
            log.info("데이터 플랫폼 전송 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
}