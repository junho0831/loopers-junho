package com.loopers.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.config.kafka.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import com.loopers.service.IdempotentEventService;
import com.loopers.service.RankingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * 실시간 랭킹 시스템을 위한 Kafka Consumer
 * 이벤트를 배치로 처리하여 Redis ZSET에 랭킹 점수를 실시간 반영
 */
@Component
public class RankingConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(RankingConsumer.class);
    
    private final RankingService rankingService;
    private final IdempotentEventService idempotentEventService;
    private final ObjectMapper objectMapper;
    
    public RankingConsumer(RankingService rankingService, 
                          IdempotentEventService idempotentEventService,
                          ObjectMapper objectMapper) {
        this.rankingService = rankingService;
        this.idempotentEventService = idempotentEventService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = {"catalog-events", "order-events"},
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "ranking-consumer-group"
    )
    @Transactional
    public void handleRankingEvents(List<ConsumerRecord<String, String>> messages,
                                   Acknowledgment acknowledgment) throws Exception {
        log.info("Processing {} events for ranking system", messages.size());
        
        int processedCount = 0;
        int skippedCount = 0;
        List<RankingService.ProductScoreUpdate> batchUpdates = new ArrayList<>();
        
        try {
            LocalDate today = LocalDate.now();
            
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
                    EventHandled.ConsumerType.RANKING,
                    version,
                    () -> {
                        RankingService.ProductScoreUpdate update = 
                            processRankingEvent(eventData, eventType, today);
                        if (update != null) {
                            batchUpdates.add(update);
                        }
                    }
                );
                
                if (processed) {
                    processedCount++;
                    log.debug("Processed ranking for event: eventId={}, eventType={}", eventId, eventType);
                } else {
                    skippedCount++;
                    log.debug("Skipped duplicate ranking event: eventId={}, eventType={}", eventId, eventType);
                }
            }
            
            // 배치로 랭킹 점수 업데이트
            if (!batchUpdates.isEmpty()) {
                rankingService.batchUpdateRankingScores(batchUpdates, today);
            }
            
            // 성공적인 처리 후 수동 확인응답
            acknowledgment.acknowledge();
            log.info("Ranking processing completed - processed: {}, skipped: {}, batch updates: {}", 
                processedCount, skippedCount, batchUpdates.size());
            
        } catch (Exception e) {
            log.error("Error processing ranking events", e);
            throw e; // 메시지가 재시도되거나 DLQ로 전송됩니다
        }
    }
    
    private RankingService.ProductScoreUpdate processRankingEvent(JsonNode eventData, String eventType, LocalDate eventDate) {
        return switch (eventType) {
            case "PRODUCT_LIKED" -> {
                Long productId = eventData.has("productId") ? eventData.get("productId").asLong() : null;
                if (productId != null) {
                    yield new RankingService.ProductScoreUpdate(productId, eventType, 1L);
                }
                yield null;
            }
            
            case "PRODUCT_UNLIKED" -> {
                Long productId = eventData.has("productId") ? eventData.get("productId").asLong() : null;
                if (productId != null) {
                    yield new RankingService.ProductScoreUpdate(productId, eventType, 1L);
                }
                yield null;
            }
            
            case "PRODUCT_VIEWED" -> {
                Long productId = eventData.has("productId") ? eventData.get("productId").asLong() : null;
                if (productId != null) {
                    yield new RankingService.ProductScoreUpdate(productId, eventType, 1L);
                }
                yield null;
            }
            
            case "ORDER_CREATED" -> {
                // 주문 이벤트는 특별 처리 (개별적으로 처리)
                processOrderEvent(eventData, eventDate);
                yield null; // 배치에서 제외 (이미 개별 처리됨)
            }
            
            default -> {
                log.debug("Ignoring event type for ranking: {}", eventType);
                yield null;
            }
        };
    }
    
    private void processOrderEvent(JsonNode eventData, LocalDate eventDate) {
        JsonNode orderEventData = eventData.get("eventData");
        if (orderEventData != null && orderEventData.has("items")) {
            JsonNode items = orderEventData.get("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    Long productId = item.has("productId") ? item.get("productId").asLong() : null;
                    Long quantity = item.has("quantity") ? item.get("quantity").asLong() : 1L;
                    Double unitPrice = item.has("unitPrice") ? item.get("unitPrice").asDouble() : null;
                    
                    if (productId != null && quantity > 0) {
                        // 주문 이벤트는 금액과 수량을 고려한 특별 점수 계산
                        rankingService.updateOrderRankingScore(productId, quantity, unitPrice, eventDate);
                        log.debug("Updated order ranking for productId={}, quantity={}, unitPrice={}", 
                            productId, quantity, unitPrice);
                    }
                }
            }
        }
    }
}
