package com.loopers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 카프카 이벤트를 타입 안전하게 처리하기 위한 기본 DTO
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "eventType",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogEventDto.class, names = {"STOCK_ADJUSTED", "PRODUCT_LIKED", "PRODUCT_UNLIKED"}),
    @JsonSubTypes.Type(value = OrderEventDto.class, names = {"ORDER_CREATED", "PAYMENT_PROCESSED"})
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class EventDto {
    private String eventId;
    private String eventType;
    private Long timestamp;
    private Long version;

    // 기본 생성자
    protected EventDto() {}

    // 유효성 검증 메서드
    public boolean hasEventId() {
        return eventId != null && !eventId.trim().isEmpty();
    }
    
    public boolean hasEventType() {
        return eventType != null && !eventType.trim().isEmpty();
    }

    // Getter/Setter
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}