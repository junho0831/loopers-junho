package com.loopers.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventLog;
import com.loopers.repository.EventLogRepository;
import com.loopers.service.IdempotentEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 모든 이벤트를 감사 로그 테이블에 기록하는 컨슈머
 * 규정 준수를 위한 완전한 감사 추적을 제공합니다
 */
@Component
public class AuditLogConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);
    
    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogConsumer(EventLogRepository eventLogRepository, ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 감사 로그는 모든 이벤트를 무조건 기록해야 하므로 멱등성 처리 없이 단순하게 처리
     * 중복 기록이 발생해도 감사 추적에는 문제없음 (오히려 더 상세한 추적 가능)
     */
    @KafkaListener(
        topics = {"catalog-events", "order-events"},
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "audit-log-consumer-group"
    )
    @Transactional
    public void handleEvents(List<ConsumerRecord<String, String>> records, 
                           Acknowledgment acknowledgment) {
        log.info("Processing {} events for audit logging", records.size());
        
        try {
            // 배치 내 중복 eventId는 한 번만 저장하여 유니크 제약 위반으로 전체 롤백되는 것을 방지
            java.util.LinkedHashMap<String, EventLog> deduped = new java.util.LinkedHashMap<>();
            for (ConsumerRecord<String, String> record : records) {
                EventLog logEntry = createEventLog(record);
                // 동일 eventId가 배치에 여러 개 있을 경우 최초 한 건만 유지
                deduped.putIfAbsent(logEntry.getEventId(), logEntry);
            }
            List<EventLog> eventLogs = new java.util.ArrayList<>(deduped.values());
                    
            eventLogRepository.saveAll(eventLogs);
            acknowledgment.acknowledge();
            
            log.info("Successfully saved {} audit log entries", eventLogs.size());

        } catch (Exception e) {
            log.error("Error processing audit log events", e);
            throw e; // 메시지가 재시도되거나 DLQ로 전송됩니다
        }
    }
    
    private EventLog createEventLog(ConsumerRecord<String, String> record) {
        try {
            // JSON에서 기본 정보 추출
            JsonNode eventData = objectMapper.readTree(record.value());
            String eventId = extractEventId(eventData);
            String eventType = extractEventType(eventData);
            String aggregateId = extractAggregateId(eventData, record.topic());
            Long version = extractVersion(eventData);
            
            return new EventLog(
                eventId,
                eventType, 
                record.topic(),
                record.key(),
                aggregateId,
                record.value(), // 원본 JSON 문자열 보존
                ZonedDateTime.now(),
                version
            );
            
        } catch (Exception e) {
            log.error("Failed to parse event record: {}", record, e);
            // 파싱 실패해도 로그는 남김 (디버깅용)
            return createFallbackEventLog(record);
        }
    }
    
    private String extractEventId(JsonNode eventData) {
        return eventData.has("eventId") ? eventData.get("eventId").asText() : "unknown";
    }
    
    private String extractEventType(JsonNode eventData) {
        return eventData.has("eventType") ? eventData.get("eventType").asText() : "unknown";
    }
    
    private String extractAggregateId(JsonNode eventData, String topic) {
        return switch (topic) {
            case "catalog-events" -> eventData.has("productId") ? eventData.get("productId").asText() : null;
            case "order-events" -> eventData.has("orderId") ? eventData.get("orderId").asText() : null;
            default -> null;
        };
    }
    
    private Long extractVersion(JsonNode eventData) {
        return eventData.has("version") ? eventData.get("version").asLong() : null;
    }
    
    private EventLog createFallbackEventLog(ConsumerRecord<String, String> record) {
        return new EventLog(
            "parse-error-" + System.currentTimeMillis(),
            "PARSE_ERROR",
            record.topic(),
            record.key(),
            null,
            record.value(),
            ZonedDateTime.now(),
            null
        );
    }
}
