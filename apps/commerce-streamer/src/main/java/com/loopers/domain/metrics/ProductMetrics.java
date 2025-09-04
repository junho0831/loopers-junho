package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 일일 제품 메트릭 집계 테이블
 * 좋아요, 판매, 조회에 대한 일일 통계를 저장합니다
 */
@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "metric_date"})
    }
)
public class ProductMetrics extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "likes_count", nullable = false)
    private Long likesCount = 0L;

    @Column(name = "likes_change", nullable = false)
    private Long likesChange = 0L;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount = 0L;

    @Column(name = "sales_change", nullable = false)
    private Long salesChange = 0L;

    @Column(name = "views_count", nullable = false)
    private Long viewsCount = 0L;

    @Column(name = "views_change", nullable = false)
    private Long viewsChange = 0L;

    // 기본 생성자
    public ProductMetrics() {
        this(0L, LocalDate.now(), 0L, 0L, 0L, 0L, 0L, 0L);
    }

    // 전체 생성자
    public ProductMetrics(Long productId, LocalDate metricDate, Long likesCount, Long likesChange,
                         Long salesCount, Long salesChange, Long viewsCount, Long viewsChange) {
        this.productId = productId;
        this.metricDate = metricDate;
        this.likesCount = likesCount;
        this.likesChange = likesChange;
        this.salesCount = salesCount;
        this.salesChange = salesChange;
        this.viewsCount = viewsCount;
        this.viewsChange = viewsChange;
    }

    /**
     * 좋아요 메트릭 업데이트
     */
    public void updateLikes(Long change) {
        this.likesChange += change;
        this.likesCount = Math.max(0L, this.likesCount + change);
    }

    /**
     * 판매 메트릭 업데이트
     */
    public void updateSales(Long change) {
        this.salesChange += change;
        this.salesCount += change;
    }

    /**
     * 조회 메트릭 업데이트
     */
    public void updateViews(Long change) {
        this.viewsChange += change;
        this.viewsCount += change;
    }

    // Getter와 Setter 메서드들
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.metricDate = metricDate;
    }

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public Long getLikesChange() {
        return likesChange;
    }

    public void setLikesChange(Long likesChange) {
        this.likesChange = likesChange;
    }

    public Long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Long salesCount) {
        this.salesCount = salesCount;
    }

    public Long getSalesChange() {
        return salesChange;
    }

    public void setSalesChange(Long salesChange) {
        this.salesChange = salesChange;
    }

    public Long getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Long viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Long getViewsChange() {
        return viewsChange;
    }

    public void setViewsChange(Long viewsChange) {
        this.viewsChange = viewsChange;
    }
}