package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "mv_product_rank_monthly",
       indexes = {
           @Index(name = "idx_mvrank_month", columnList = "`year_month`"),
           @Index(name = "idx_mvrank_month_rank", columnList = "`year_month`, `rank_no`")
       })
public class MvProductRankMonthly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Month key, e.g. 202509
    @Column(name = "`year_month`", nullable = false, length = 6)
    private String yearMonth;

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

    public MvProductRankMonthly() {}

    public MvProductRankMonthly(Long productId, String yearMonth, Integer rank, Double score,
                                Long likeCount, Long orderCount, Long viewCount) {
        this.productId = productId;
        this.yearMonth = yearMonth;
        this.rank = rank;
        this.score = score;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.viewCount = viewCount;
    }

    public Long getProductId() { return productId; }
    public String getYearMonth() { return yearMonth; }
    public Integer getRank() { return rank; }
    public Double getScore() { return score; }
    public Long getLikeCount() { return likeCount; }
    public Long getOrderCount() { return orderCount; }
    public Long getViewCount() { return viewCount; }
}
