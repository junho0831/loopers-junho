package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.ZonedDateTime;

/**
 * 감사 목적의 이벤트 로그 테이블
 * 컨슈머에 의해 처리된 모든 이벤트를 저장합니다
 */
@Entity
@Table(name = "event_log")
public class EventLog extends BaseEntity {
    
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "processed_at", nullable = false)
    private ZonedDateTime processedAt;

    @Column(name = "version")
    private Long version;

    // 기본 생성자
    public EventLog() {
        this("", "", "", null, null, "", ZonedDateTime.now(), null);
    }

    // 전체 생성자
    public EventLog(String eventId, String eventType, String topic, String partitionKey, 
                   String aggregateId, String eventData, ZonedDateTime processedAt, Long version) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.aggregateId = aggregateId;
        this.eventData = eventData;
        this.processedAt = processedAt;
        this.version = version;
    }

    // Getter와 Setter 메서드들
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public ZonedDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}