package com.loopers.domain.ranking;

/**
 * 랭킹 정보를 담는 도메인 모델
 * 상품 ID, 점수, 순위 정보를 포함합니다
 */
public class RankingItem {
    
    private final Long productId;
    private final Double score;
    private final Long rank;
    
    public RankingItem(Long productId, Double score, Long rank) {
        this.productId = productId;
        this.score = score;
        this.rank = rank;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public Double getScore() {
        return score;
    }
    
    public Long getRank() {
        return rank;
    }
    
    @Override
    public String toString() {
        return String.format("RankingItem{productId=%d, score=%.2f, rank=%d}", 
            productId, score, rank);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RankingItem that = (RankingItem) o;
        return productId.equals(that.productId) && 
               rank.equals(that.rank);
    }
    
    @Override
    public int hashCode() {
        return productId.hashCode() * 31 + rank.hashCode();
    }
}