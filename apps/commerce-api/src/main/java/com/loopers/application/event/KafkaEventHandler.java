package com.loopers.application.event;

import com.loopers.application.product.ProductFacade;
import com.loopers.domain.event.CatalogEvent;
import com.loopers.domain.event.OrderEvent;
import com.loopers.domain.like.ProductLikeEvent;
import com.loopers.domain.order.OrderCreatedEvent;
import com.loopers.domain.payment.PaymentResultEvent;
import com.loopers.infrastructure.event.KafkaEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 스프링 애플리케이션 이벤트를 카프카 이벤트로 변환
 */
@Component
public class KafkaEventHandler {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventHandler.class);
    
    private final KafkaEventPublisher kafkaEventPublisher;

    public KafkaEventHandler(KafkaEventPublisher kafkaEventPublisher) {
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    /**
     * ProductLikeEvent를 처리하여 catalog-events 토픽으로 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductLikeEvent(ProductLikeEvent event) {
        try {
            CatalogEvent catalogEvent;
            if (event.isLiked()) {
                catalogEvent = CatalogEvent.productLiked(
                    event.getProductId(), 
                    event.getUserId(), 
                    event.getTimestamp().toEpochSecond()
                );
            } else {
                catalogEvent = CatalogEvent.productUnliked(
                    event.getProductId(), 
                    event.getUserId(), 
                    event.getTimestamp().toEpochSecond()
                );
            }
            
            kafkaEventPublisher.publishCatalogEvent(catalogEvent);
            
        } catch (Exception e) {
            log.error("Failed to handle ProductLikeEvent: {}", event, e);
            // 재시도 로직이나 데드 레터 큐 구현 고려
        }
    }

    /**
     * OrderCreatedEvent를 처리하여 order-events 토픽으로 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            // 데모용으로 샘플 주문 항목 생성
            // 실제 시나리오에서는 주문과 주문 항목을 조회해야 함
            List<OrderEvent.OrderItemData> items = List.of(
                // 여기에서 실제 주문 항목을 로드해야 함
            );
            
            OrderEvent orderEvent = OrderEvent.orderCreated(
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                event.getCouponId(),
                event.getCardCompany(),
                event.getCardNumber(),
                items
            );
            
            kafkaEventPublisher.publishOrderEvent(orderEvent);
            
        } catch (Exception e) {
            log.error("Failed to handle OrderCreatedEvent: {}", event, e);
            // 재시도 로직이나 데드 레터 큐 구현 고려
        }
    }

    /**
     * PaymentResultEvent를 처리하여 order-events 토픽으로 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentResultEvent(PaymentResultEvent event) {
        try {
            OrderEvent orderEvent = OrderEvent.paymentProcessed(
                Long.parseLong(event.getOrderId()),
                event.getTransactionId(),
                event.getAmount(),
                event.getStatus().toString(),
                event.getFailureReason()
            );
            
            kafkaEventPublisher.publishOrderEvent(orderEvent);
            
        } catch (Exception e) {
            log.error("Failed to handle PaymentResultEvent: {}", event, e);
            // 재시도 로직이나 데드 레터 큐 구현 고려
        }
    }

    /**
     * 재고 조정 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockAdjustedEvent(ProductFacade.StockAdjustedEvent event) {
        try {
            CatalogEvent catalogEvent = CatalogEvent.stockAdjusted(
                event.getProductId(), 
                event.getQuantityChanged(), 
                System.currentTimeMillis()
            );
            
            kafkaEventPublisher.publishCatalogEvent(catalogEvent);
            
        } catch (Exception e) {
            log.error("Failed to handle stock adjustment for productId: {}, quantity: {}", 
                event.getProductId(), event.getQuantityChanged(), e);
        }
    }
}
