package com.loopers.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.event.EventHandled;
import com.loopers.service.IdempotentEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

/**
 * 카탈로그 이벤트를 기반으로 한 캐시 무효화 컨슈머
 * 제품 상태 변경(좋아요, 재고 등) 시 캐시된 데이터를 제거합니다
 */
@Component
public class CacheInvalidationConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationConsumer.class);
    
    private final IdempotentEventService idempotentEventService;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInvalidationConsumer(IdempotentEventService idempotentEventService,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.idempotentEventService = idempotentEventService;
        this.redisTemplate = redisTemplate;
    }
    
    @KafkaListener(
        topics = {"catalog-events"},  // 캐시 무효화를 위해 카탈로그 이벤트만 수신
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "cache-invalidation-consumer-group"
    )
    @Transactional
    public void cacheInvalidationListener(List<ConsumerRecord<String, JsonNode>> messages,
                                        Acknowledgment acknowledgment) {
        log.info("Processing {} events for cache invalidation", messages.size());
        
        int processedCount = 0;
        int skippedCount = 0;
        
        try {
            for (ConsumerRecord<String, JsonNode> message : messages) {
                JsonNode eventData = message.value();
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
                    EventHandled.ConsumerType.CACHE_INVALIDATION,
                    version,
                    () -> processCacheInvalidationEvent(eventData, eventType)
                );
                
                if (processed) {
                    processedCount++;
                    log.debug("Processed cache invalidation for event: eventId={}, eventType={}", eventId, eventType);
                } else {
                    skippedCount++;
                    log.debug("Skipped duplicate cache invalidation event: eventId={}, eventType={}", eventId, eventType);
                }
            }
            
            // 성공적인 처리 후 수동 확인응답
            acknowledgment.acknowledge();
            log.info("Cache invalidation completed - processed: {}, skipped: {}", processedCount, skippedCount);
            
        } catch (Exception e) {
            log.error("Error processing cache invalidation events", e);
            throw e; // 메시지가 재시도되거나 DLQ로 전송됩니다
        }
    }
    
    private void processCacheInvalidationEvent(JsonNode eventData, String eventType) {
        Long productId = eventData.has("productId") ? eventData.get("productId").asLong() : null;
        if (productId == null) {
            log.warn("Skipping cache invalidation - missing productId in event: {}", eventData);
            return;
        }
        
        switch (eventType) {
            case "PRODUCT_LIKED":
            case "PRODUCT_UNLIKED":
                // 제품 상세 캐시 무효화 (좋아요 수 변경됨)
                invalidateProductCache(productId, "Product likes changed");
                break;
                
            case "STOCK_ADJUSTED":
                JsonNode stockData = eventData.get("eventData");
                if (stockData != null && stockData.has("quantity")) {
                    int quantity = stockData.get("quantity").asInt();
                    // 재고가 0이 되거나 재고가 보충될 때 캐시 무효화
                    if (quantity <= 0) {
                        invalidateProductCache(productId, "Stock depleted");
                        invalidateProductListCaches(); // 품절 시 제품이 목록에서 제외될 수 있음
                    } else {
                        invalidateProductCache(productId, "Stock replenished");
                        invalidateProductListCaches(); // 제품이 목록에 다시 포함될 수 있음
                    }
                }
                break;
                
            case "PRODUCT_VIEWED":
                // 제품 조회의 경우, 일반적으로 캐시를 무효화하지 않습니다
                // 이는 단순히 메트릭 추적용입니다
                log.debug("Product {} viewed - no cache invalidation needed", productId);
                break;
                
            default:
                log.debug("No cache invalidation needed for event type: {}", eventType);
                break;
        }
    }
    
    /**
     * 제품별 캐시 무효화
     */
    private void invalidateProductCache(Long productId, String reason) {
        try {
            // 제품별 캐시 키 패턴
            String productCachePattern = "*product:" + productId + "*";
            Set<String> keysToDelete = redisTemplate.keys(productCachePattern);
            
            if (keysToDelete != null && !keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("Invalidated {} cache keys for productId={} - reason: {}", 
                    keysToDelete.size(), productId, reason);
                log.debug("Deleted cache keys: {}", keysToDelete);
            } else {
                log.debug("No cache keys found for productId={} - pattern: {}", productId, productCachePattern);
            }
            
            // 특정 캐시 항목도 무효화
            invalidateSpecificProductCaches(productId);
            
        } catch (Exception e) {
            log.error("Failed to invalidate cache for productId={}", productId, e);
            throw e;
        }
    }
    
    /**
     * 특정 제품 캐시 항목 무효화
     */
    private void invalidateSpecificProductCaches(Long productId) {
        try {
            // 제품에 대한 일반적인 캐시 키 패턴
            String[] cacheKeys = {
                "product:detail:" + productId,
                "product:info:" + productId,
                "product:stock:" + productId,
                "product:likes:" + productId
            };
            
            for (String cacheKey : cacheKeys) {
                Boolean deleted = redisTemplate.delete(cacheKey);
                if (Boolean.TRUE.equals(deleted)) {
                    log.debug("Deleted specific cache key: {}", cacheKey);
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to invalidate specific product caches for productId={}", productId, e);
            // 이것은 중요하지 않으므로 여기서 예외를 발생시키지 않습니다
        }
    }
    
    /**
     * 제품 목록 캐시 무효화 (재고 가용성 변경 시)
     */
    private void invalidateProductListCaches() {
        try {
            // 제품 목록 캐시 키 패턴
            String listCachePattern = "*product:list*";
            Set<String> keysToDelete = redisTemplate.keys(listCachePattern);
            
            if (keysToDelete != null && !keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("Invalidated {} product list cache keys", keysToDelete.size());
                log.debug("Deleted product list cache keys: {}", keysToDelete);
            }
            
            // 일반적인 목록 캐시 항목도 무효화
            String[] commonListKeys = {
                "products:all",
                "products:popular",
                "products:featured",
                "products:categories"
            };
            
            for (String cacheKey : commonListKeys) {
                Boolean deleted = redisTemplate.delete(cacheKey);
                if (Boolean.TRUE.equals(deleted)) {
                    log.debug("Deleted common list cache key: {}", cacheKey);
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to invalidate product list caches", e);
            // 이것은 중요하지 않으므로 여기서 예외를 발생시키지 않습니다
        }
    }
}