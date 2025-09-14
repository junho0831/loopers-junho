package com.loopers.interfaces.api;

import com.loopers.domain.product.Product;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.ranking.RankingItem;

/**
 * 상품 상세 응답 + 랭킹 정보를 함께 제공하는 DTO
 * 기존 캐시 가능한 상품 상세(ProductDetailResponse)에 랭킹 필드만 얹어서 반환
 */
public class ProductDetailWithRankingResponse {

    private final Long productId;
    private final String productName;
    private final long price;
    private final int stockQuantity;
    private final long likeCount;
    private final Long brandId;
    private final String brandName;

    // 랭킹 정보 (없으면 null)
    private final Double rankingScore;
    private final Long ranking;

    private ProductDetailWithRankingResponse(Long productId, String productName, long price, int stockQuantity,
                                            long likeCount, Long brandId, String brandName,
                                            Double rankingScore, Long ranking) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.likeCount = likeCount;
        this.brandId = brandId;
        this.brandName = brandName;
        this.rankingScore = rankingScore;
        this.ranking = ranking;
    }

    public static ProductDetailWithRankingResponse from(ProductDetailResponse detail, RankingItem rankingItem) {
        return new ProductDetailWithRankingResponse(
            detail.getProductId(),
            detail.getProductName(),
            detail.getPrice(),
            detail.getStockQuantity(),
            detail.getLikeCount(),
            detail.getBrandId(),
            detail.getBrandName(),
            rankingItem != null ? rankingItem.getScore() : null,
            rankingItem != null ? rankingItem.getRank() : null
        );
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public long getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public long getLikeCount() { return likeCount; }
    public Long getBrandId() { return brandId; }
    public String getBrandName() { return brandName; }
    public Double getRankingScore() { return rankingScore; }
    public Long getRanking() { return ranking; }
}

