package com.loopers.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 카탈로그 관련 변경사항(제품, 좋아요 등)에 대한 도메인 이벤트
 */
public class CatalogEvent {
    private String eventId;
    private String eventType;
    private Long productId;
    private String userId;
    private Long timestamp;
    private Integer quantityChanged;
    private Long version;

    // 빌더 패턴을 위한 private 생성자
    private CatalogEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.version = System.currentTimeMillis();
    }

    public static CatalogEvent productLiked(Long productId, String userId, Long timestamp) {
        CatalogEvent event = new CatalogEvent();
        event.eventType = "PRODUCT_LIKED";
        event.productId = productId;
        event.userId = userId;
        event.timestamp = timestamp;
        return event;
    }

    public static CatalogEvent productUnliked(Long productId, String userId, Long timestamp) {
        CatalogEvent event = new CatalogEvent();
        event.eventType = "PRODUCT_UNLIKED";
        event.productId = productId;
        event.userId = userId;
        event.timestamp = timestamp;
        return event;
    }

    public static CatalogEvent stockAdjusted(Long productId, Integer quantityChanged, Long timestamp) {
        CatalogEvent event = new CatalogEvent();
        event.eventType = "STOCK_ADJUSTED";
        event.productId = productId;
        event.quantityChanged = quantityChanged;
        event.timestamp = timestamp;
        return event;
    }

    // Getter 메서드들
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Long getProductId() { return productId; }
    public String getUserId() { return userId; }
    public Long getTimestamp() { return timestamp; }
    public Integer getQuantityChanged() { return quantityChanged; }
    public Long getVersion() { return version; }

    @Override
    public String toString() {
        return "CatalogEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", productId=" + productId +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", quantityChanged=" + quantityChanged +
                ", version=" + version +
                '}';
    }
}