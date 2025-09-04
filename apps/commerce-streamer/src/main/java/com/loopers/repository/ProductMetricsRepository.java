package com.loopers.repository;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {
    ProductMetrics findByProductIdAndMetricDate(Long productId, LocalDate metricDate);
    
    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, metric_date, likes_count, likes_change, sales_count, sales_change, views_count, views_change, created_at, updated_at)
        VALUES (:productId, :metricDate, :likesCount, :likesChange, :salesCount, :salesChange, :viewsCount, :viewsChange, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
        likes_count = likes_count + VALUES(likes_change),
        likes_change = likes_change + VALUES(likes_change),
        sales_count = sales_count + VALUES(sales_change),
        sales_change = sales_change + VALUES(sales_change),
        views_count = views_count + VALUES(views_change),
        views_change = views_change + VALUES(views_change),
        updated_at = NOW()
    """, nativeQuery = true)
    void upsertMetrics(
        @Param("productId") Long productId,
        @Param("metricDate") LocalDate metricDate,
        @Param("likesCount") Long likesCount,
        @Param("likesChange") Long likesChange,
        @Param("salesCount") Long salesCount,
        @Param("salesChange") Long salesChange,
        @Param("viewsCount") Long viewsCount,
        @Param("viewsChange") Long viewsChange
    );
    
    List<ProductMetrics> findByProductIdAndMetricDateBetween(
        Long productId,
        LocalDate startDate,
        LocalDate endDate
    );
}