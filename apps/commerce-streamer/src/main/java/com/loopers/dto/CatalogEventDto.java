package com.loopers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Catalog 이벤트를 위한 타입 안전한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEventDto extends EventDto {
    private Long productId;
    private String userId;
    private Integer quantityChanged;
    private String eventData; // 추가 이벤트 데이터
    
    // 재고 관련 필드들 - 타입 안전성을 위해 추가
    private Integer quantity;
    private Integer previousQuantity;

    // 기본 생성자
    public CatalogEventDto() {}

    // 필드별 검증 메서드
    public boolean hasProductId() {
        return productId != null;
    }
    
    public boolean hasUserId() {
        return userId != null && !userId.trim().isEmpty();
    }
    
    public boolean hasQuantityChanged() {
        return quantityChanged != null;
    }
    
    public boolean hasQuantity() {
        return quantity != null;
    }
    
    public boolean hasPreviousQuantity() {
        return previousQuantity != null;
    }

    // Getter/Setter
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Integer getQuantityChanged() { return quantityChanged; }
    public void setQuantityChanged(Integer quantityChanged) { this.quantityChanged = quantityChanged; }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }

    @Override
    public String toString() {
        return "CatalogEventDto{" +
                "eventId='" + getEventId() + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", productId=" + productId +
                ", userId='" + userId + '\'' +
                ", quantityChanged=" + quantityChanged +
                ", quantity=" + quantity +
                ", previousQuantity=" + previousQuantity +
                '}';
    }
}