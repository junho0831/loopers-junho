package com.loopers.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.repository.ProductMetricsRepository;
import com.loopers.service.IdempotentEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/**
 * 제품의 일일 메트릭 집계를 위한 컨슈머
 * 좋아요, 판매, 조회 이벤트를 처리하여 일일 통계를 업데이트합니다
 */
@Component
public class MetricsAggregationConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(MetricsAggregationConsumer.class);
    
    private final ProductMetricsRepository productMetricsRepository;
    private final IdempotentEventService idempotentEventService;
    private final ObjectMapper objectMapper;

    public MetricsAggregationConsumer(ProductMetricsRepository productMetricsRepository,
                                     IdempotentEventService idempotentEventService,
                                     ObjectMapper objectMapper) {
        this.productMetricsRepository = productMetricsRepository;
        this.idempotentEventService = idempotentEventService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = {"catalog-events", "order-events"},
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "metrics-aggregation-consumer-group"
    )
    @Transactional
    public void metricsAggregationListener(List<ConsumerRecord<String, String>> messages,
                                          Acknowledgment acknowledgment) throws Exception {
        log.info("Processing {} events for metrics aggregation", messages.size());
        
        int processedCount = 0;
        int skippedCount = 0;
        
        try {
            for (ConsumerRecord<String, String> message : messages) {
                JsonNode eventData = objectMapper.readTree(message.value());
                String eventId = eventData.has("eventId") ? eventData.get("eventId").asText() : null;
                String eventType = eventData.has("eventType") ? eventData.get("eventType").asText() : null;
                Long version = eventData.has("version") ? eventData.get("version").asLong() : null;
                
                if (eventId == null || eventType == null) {
                    log.warn("Skipping invalid event - missing eventId or eventType: {}", eventData);
                    skippedCount++;
                    continue;
                }
                
                boolean processed = idempotentEventService.processEventIdempotent(
                    eventId,
                    EventHandled.ConsumerType.METRICS_AGGREGATION,
                    version,
                    () -> processMetricsEvent(eventData, eventType)
                );
                
                if (processed) {
                    processedCount++;
                    log.debug("Processed metrics for event: eventId={}, eventType={}", eventId, eventType);
                } else {
                    skippedCount++;
                    log.debug("Skipped duplicate metrics event: eventId={}, eventType={}", eventId, eventType);
                }
            }
            
            // 성공적인 처리 후 수동 확인응답
            acknowledgment.acknowledge();
            log.info("Metrics aggregation completed - processed: {}, skipped: {}", processedCount, skippedCount);
            
        } catch (Exception e) {
            log.error("Error processing metrics aggregation events", e);
            throw e; // 메시지가 재시도되거나 DLQ로 전송됩니다
        }
    }
    
    private void processMetricsEvent(JsonNode eventData, String eventType) {
        switch (eventType) {
            case "PRODUCT_LIKED":
                Long productIdLiked = eventData.has("productId") ? eventData.get("productId").asLong() : null;
                if (productIdLiked != null) {
                    updateProductMetrics(productIdLiked, 1L, 0L, 0L);
                }
                break;
                
            case "PRODUCT_UNLIKED":
                Long productIdUnliked = eventData.has("productId") ? eventData.get("productId").asLong() : null;
                if (productIdUnliked != null) {
                    updateProductMetrics(productIdUnliked, -1L, 0L, 0L);
                }
                break;
                
            case "PRODUCT_VIEWED":
                Long productIdViewed = eventData.has("productId") ? eventData.get("productId").asLong() : null;
                if (productIdViewed != null) {
                    updateProductMetrics(productIdViewed, 0L, 0L, 1L);
                }
                break;
                
            case "ORDER_CREATED":
                JsonNode orderEventData = eventData.get("eventData");
                if (orderEventData != null && orderEventData.has("items")) {
                    JsonNode items = orderEventData.get("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            Long productId = item.has("productId") ? item.get("productId").asLong() : null;
                            Long quantity = item.has("quantity") ? item.get("quantity").asLong() : 1L;
                            if (productId != null) {
                                updateProductMetrics(productId, 0L, quantity, 0L);
                            }
                        }
                    }
                }
                break;
                
            default:
                log.debug("Ignoring event type for metrics: {}", eventType);
                break;
        }
    }
    
    private void updateProductMetrics(Long productId, Long likesChange, Long salesChange, Long viewsChange) {
        LocalDate today = LocalDate.now();
        
        try {
            // 오늘에 대한 기존 메트릭 찾기 시도
            ProductMetrics existingMetrics = productMetricsRepository.findByProductIdAndMetricDate(productId, today);
            
            if (existingMetrics != null) {
                // 기존 메트릭 업데이트
                existingMetrics.updateLikes(likesChange);
                existingMetrics.updateSales(salesChange);
                existingMetrics.updateViews(viewsChange);
                productMetricsRepository.save(existingMetrics);
                
                log.debug("Updated metrics for productId={}, likesChange={}, salesChange={}, viewsChange={}", 
                    productId, likesChange, salesChange, viewsChange);
            } else {
                // 새로운 메트릭 항목 생성
                ProductMetrics newMetrics = new ProductMetrics(
                    productId,
                    today,
                    Math.max(0L, likesChange),
                    likesChange,
                    Math.max(0L, salesChange),
                    salesChange,
                    Math.max(0L, viewsChange),
                    viewsChange
                );
                productMetricsRepository.save(newMetrics);
                
                log.debug("Created new metrics for productId={}, likesChange={}, salesChange={}, viewsChange={}", 
                    productId, likesChange, salesChange, viewsChange);
            }
            
        } catch (Exception e) {
            log.error("Error updating metrics for productId={}", productId, e);
            throw e;
        }
    }
}
