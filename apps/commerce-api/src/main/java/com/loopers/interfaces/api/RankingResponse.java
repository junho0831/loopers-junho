package com.loopers.interfaces.api;

import com.loopers.domain.product.Product;
import com.loopers.domain.ranking.RankingItem;

/**
 * 랭킹 API 응답을 위한 DTO
 * 상품 정보와 랭킹 정보를 함께 제공합니다
 */
public class RankingResponse {
    
    private final Long productId;
    private final String productName;
    // description 제거 - Product에 없는 필드
    private final Double price;
    private final Long stockQuantity;
    private final Double score;
    private final Long rank;
    
    public RankingResponse(Product product, RankingItem rankingItem) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.price = (double) product.getPrice().getValue();
        this.stockQuantity = (long) product.getStock().getQuantity();
        this.score = rankingItem.getScore();
        this.rank = rankingItem.getRank();
    }
    
    // 랭킹 정보가 없는 경우
    public RankingResponse(Product product) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.price = (double) product.getPrice().getValue();
        this.stockQuantity = (long) product.getStock().getQuantity();
        this.score = null;
        this.rank = null;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    // description getter 제거
    
    public Double getPrice() {
        return price;
    }
    
    public Long getStockQuantity() {
        return stockQuantity;
    }
    
    public Double getScore() {
        return score;
    }
    
    public Long getRank() {
        return rank;
    }
}