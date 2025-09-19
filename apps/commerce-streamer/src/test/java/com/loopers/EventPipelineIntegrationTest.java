package com.loopers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventLog;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.repository.EventLogRepository;
import com.loopers.repository.ProductMetricsRepository;
import com.loopers.service.EventTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 완전한 Kafka 이벤트 파이프라인에 대한 통합 테스트
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"catalog-events", "order-events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@Transactional
class EventPipelineIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private EventLogRepository eventLogRepository;
    
    @Autowired
    private ProductMetricsRepository productMetricsRepository;
    
    @Autowired
    private EventTestService eventTestService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProductLikeEventPipeline() throws Exception {
        // Given - 제품 좋아요 이벤트 생성
        String eventId = UUID.randomUUID().toString();
        Long productId = 123L;
        String userId = "user456";
        
        Map<String, Object> catalogEvent = createCatalogEvent(eventId, "PRODUCT_LIKED", productId, userId);
        
        // When - Kafka로 이벤트 전송
        kafkaTemplate.send("catalog-events", String.valueOf(productId), objectMapper.writeValueAsString(catalogEvent));
        
        // Then - Wait for event to be processed by all consumers
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // 감사 로그 검증
            assertTrue(eventLogRepository.existsByEventId(eventId), 
                "Event should be logged in audit log");
            
            // 메트릭이 업데이트되었는지 검증
            ProductMetrics metrics = productMetricsRepository.findByProductIdAndMetricDate(
                productId, LocalDate.now());
            assertNotNull(metrics, "Product metrics should be created");
            assertEquals(1L, metrics.getLikesChange(), "Likes change should be 1");
        });
        
        // Verify event test service
        assertTrue(eventTestService.verifyEventProcessing(eventId),
            "Event should be verified as processed");
    }
    
    @Test
    void testProductUnlikeEventPipeline() throws Exception {
        // Given - Create a product unlike event
        String eventId = UUID.randomUUID().toString();
        Long productId = 124L;
        String userId = "user789";
        
        Map<String, Object> catalogEvent = createCatalogEvent(eventId, "PRODUCT_UNLIKED", productId, userId);
        
        // When - Kafka로 이벤트 전송
        kafkaTemplate.send("catalog-events", String.valueOf(productId), objectMapper.writeValueAsString(catalogEvent));
        
        // Then - Wait for event to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // 감사 로그 검증
            EventLog eventLog = eventLogRepository.findByEventIdAndEventType(eventId, "PRODUCT_UNLIKED");
            assertNotNull(eventLog, "Event should be in audit log");
            assertEquals("catalog-events", eventLog.getTopic(), "Topic should be catalog-events");
            
            // 메트릭이 업데이트되었는지 검증 (unlike decreases likes)
            ProductMetrics metrics = productMetricsRepository.findByProductIdAndMetricDate(
                productId, LocalDate.now());
            assertNotNull(metrics, "Product metrics should be created");
            assertEquals(-1L, metrics.getLikesChange(), "Likes change should be -1");
        });
    }
    
    @Test
    void testDuplicateEventIdempotency() throws Exception {
        // Given - Create an event
        String eventId = UUID.randomUUID().toString();
        Long productId = 125L;
        String userId = "user999";
        
        Map<String, Object> catalogEvent = createCatalogEvent(eventId, "PRODUCT_LIKED", productId, userId);
        
        // When - Send the same event twice
        kafkaTemplate.send("catalog-events", String.valueOf(productId), objectMapper.writeValueAsString(catalogEvent));
        kafkaTemplate.send("catalog-events", String.valueOf(productId), objectMapper.writeValueAsString(catalogEvent));
        
        // Then - Wait and verify only processed once
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Should only have one audit log entry
            long auditCount = eventLogRepository.findAll().stream()
                .filter(log -> eventId.equals(log.getEventId()))
                .count();
            assertEquals(1L, auditCount, "Should only have one audit log entry");
            
            // Should only have one metrics update
            ProductMetrics metrics = productMetricsRepository.findByProductIdAndMetricDate(
                productId, LocalDate.now());
            assertNotNull(metrics, "Product metrics should be created");
            assertEquals(1L, metrics.getLikesChange(), "Should only process once - likes change should be 1");
        });
    }
    
    @Test
    void testEventProcessingStats() throws Exception {
        // Given - Some initial state
        EventTestService.EventProcessingStats initialStats = eventTestService.getProcessingStats();
        
        // When - Send an event
        String eventId = UUID.randomUUID().toString();
        Long productId = 126L;
        String userId = "user111";
        
        Map<String, Object> catalogEvent = createCatalogEvent(eventId, "PRODUCT_VIEWED", productId, userId);
        kafkaTemplate.send("catalog-events", String.valueOf(productId), objectMapper.writeValueAsString(catalogEvent));
        
        // Then - Stats should be updated
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            EventTestService.EventProcessingStats currentStats = eventTestService.getProcessingStats();
            assertTrue(currentStats.getTotalEventsProcessed() > initialStats.getTotalEventsProcessed(),
                "Total events processed should increase");
        });
    }
    
    private Map<String, Object> createCatalogEvent(String eventId, String eventType, Long productId, String userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", eventType);
        event.put("productId", productId);
        event.put("userId", userId);
        event.put("timestamp", java.time.ZonedDateTime.now().toString());
        event.put("version", System.currentTimeMillis());
        
        // 타입에 따른 이벤트 데이터
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("productId", productId);
        eventData.put("userId", userId);
        eventData.put("action", eventType.equals("PRODUCT_LIKED") ? "LIKED" : 
                               eventType.equals("PRODUCT_UNLIKED") ? "UNLIKED" : "VIEWED");
        event.put("eventData", eventData);
        
        return event;
    }
}
