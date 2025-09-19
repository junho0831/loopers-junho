package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "mv_product_rank_weekly",
       indexes = {
           @Index(name = "idx_mvrank_week", columnList = "`year_week`"),
           @Index(name = "idx_mvrank_week_rank", columnList = "`year_week`, `rank_no`")
       })
public class MvProductRankWeekly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // ISO week key, e.g. 2025W37
    @Column(name = "`year_week`", nullable = false, length = 8)
    private String yearWeek;

    @Column(name = "`rank_no`", nullable = false)
    private Integer rank;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "order_count", nullable = false)
    private Long orderCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    public MvProductRankWeekly() {}

    public MvProductRankWeekly(Long productId, String yearWeek, Integer rank, Double score,
                               Long likeCount, Long orderCount, Long viewCount) {
        this.productId = productId;
        this.yearWeek = yearWeek;
        this.rank = rank;
        this.score = score;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.viewCount = viewCount;
    }

    public Long getProductId() { return productId; }
    public String getYearWeek() { return yearWeek; }
    public Integer getRank() { return rank; }
    public Double getScore() { return score; }
    public Long getLikeCount() { return likeCount; }
    public Long getOrderCount() { return orderCount; }
    public Long getViewCount() { return viewCount; }
}
