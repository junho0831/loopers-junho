package com.loopers.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.CatalogEvent;
import com.loopers.domain.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 카프카 토픽에 이벤트를 발행하기 위한 인프라 서비스
 */
@Component
public class KafkaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    private static final String CATALOG_EVENTS_TOPIC = "catalog-events";
    private static final String ORDER_EVENTS_TOPIC = "order-events";
    
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCatalogEvent(CatalogEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String partitionKey = event.getProductId().toString();
            
            kafkaTemplate.send(CATALOG_EVENTS_TOPIC, partitionKey, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish catalog event: {}", event, ex);
                        } else {
                            log.info("Successfully published catalog event: {} to partition: {}", 
                                    event.getEventId(), result.getRecordMetadata().partition());
                        }
                    });
                    
        } catch (Exception e) {
            log.error("Failed to serialize catalog event: {}", event, e);
        }
    }

    public void publishOrderEvent(OrderEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String partitionKey = event.getOrderId().toString();
            
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, partitionKey, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish order event: {}", event, ex);
                        } else {
                            log.info("Successfully published order event: {} to partition: {}", 
                                    event.getEventId(), result.getRecordMetadata().partition());
                        }
                    });
                    
        } catch (Exception e) {
            log.error("Failed to serialize order event: {}", event, e);
        }
    }
}