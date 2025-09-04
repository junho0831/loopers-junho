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
 * 처리된 모든 이벤트의 완전한 감사 추적을 제공합니다
 */
@Component
public class AuditLogConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);
    
    private final EventLogRepository eventLogRepository;
    private final IdempotentEventService idempotentEventService;
    private final ObjectMapper objectMapper;

    public AuditLogConsumer(EventLogRepository eventLogRepository,
                           IdempotentEventService idempotentEventService,
                           ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.idempotentEventService = idempotentEventService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = {"catalog-events", "order-events"},
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "audit-log-consumer-group"
    )
    @Transactional
    public void auditEventListener(List<ConsumerRecord<String, JsonNode>> messages, 
                                  Acknowledgment acknowledgment) {
        log.info("Processing {} events for audit logging", messages.size());
        
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
                    EventHandled.ConsumerType.AUDIT_LOG,
                    version,
                    () -> saveEventLog(message, eventId, eventType, eventData, version)
                );
                
                if (processed) {
                    processedCount++;
                    log.debug("Audit logged event: eventId={}, eventType={}", eventId, eventType);
                } else {
                    skippedCount++;
                    log.debug("Skipped duplicate event: eventId={}, eventType={}", eventId, eventType);
                }
            }
            
            // 성공적인 처리 후 수동 확인응답
            acknowledgment.acknowledge();
            log.info("Audit log processing completed - processed: {}, skipped: {}", processedCount, skippedCount);
            
        } catch (Exception e) {
            log.error("Error processing audit log events", e);
            throw e; // 메시지가 재시도되거나 DLQ로 전송됩니다
        }
    }
    
    private void saveEventLog(ConsumerRecord<String, JsonNode> message, String eventId, 
                             String eventType, JsonNode eventData, Long version) {
        String aggregateId = null;
        switch (message.topic()) {
            case "catalog-events":
                aggregateId = eventData.has("productId") ? eventData.get("productId").asText() : null;
                break;
            case "order-events":
                aggregateId = eventData.has("orderId") ? eventData.get("orderId").asText() : null;
                break;
        }
        
        try {
            EventLog eventLog = new EventLog(
                eventId,
                eventType,
                message.topic(),
                message.key(),
                aggregateId,
                objectMapper.writeValueAsString(eventData),
                ZonedDateTime.now(),
                version
            );
            
            eventLogRepository.save(eventLog);
            log.trace("Event log saved - eventId: {}, topic: {}, partition: {}", 
                eventId, message.topic(), message.partition());
        } catch (Exception e) {
            log.error("Failed to save event log for eventId: {}", eventId, e);
            throw new RuntimeException("Failed to save event log", e);
        }
    }
}