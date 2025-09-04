package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.ZonedDateTime;

/**
 * 멱등성 처리를 위한 이벤트 처리 추적 테이블
 * 동일한 이벤트의 중복 처리를 방지합니다
 */
@Entity
@Table(
    name = "event_handled",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "consumer_type"})
    }
)
public class EventHandled extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "consumer_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConsumerType consumerType;

    @Column(name = "handled_at", nullable = false)
    private ZonedDateTime handledAt;

    @Column(name = "version")
    private Long version;

    // 기본 생성자
    public EventHandled() {
        this("", ConsumerType.AUDIT_LOG, ZonedDateTime.now(), null);
    }

    // 전체 생성자
    public EventHandled(String eventId, ConsumerType consumerType, ZonedDateTime handledAt, Long version) {
        this.eventId = eventId;
        this.consumerType = consumerType;
        this.handledAt = handledAt;
        this.version = version;
    }

    // 컴슈머 타입을 위한 Enum
    public enum ConsumerType {
        AUDIT_LOG,
        METRICS_AGGREGATION,
        CACHE_INVALIDATION
    }

    // Getter와 Setter 메서드들
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public ConsumerType getConsumerType() {
        return consumerType;
    }

    public void setConsumerType(ConsumerType consumerType) {
        this.consumerType = consumerType;
    }

    public ZonedDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(ZonedDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}