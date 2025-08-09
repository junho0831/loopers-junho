package com.loopers.domain.product;

import org.springframework.data.domain.Sort;

public enum ProductSortType {
    LATEST_DESC("createdAt", Sort.Direction.DESC),      // 최신순 (내림차순)
    LATEST_ASC("createdAt", Sort.Direction.ASC),       // 오래된순 (오름차순)
    PRICE_ASC("price.value", Sort.Direction.ASC),      // 가격 낮은순
    PRICE_DESC("price.value", Sort.Direction.DESC),    // 가격 높은순
    LIKES_ASC("likesCount", Sort.Direction.ASC),       // 좋아요 적은순
    LIKES_DESC("likesCount", Sort.Direction.DESC);     // 좋아요 많은순

    private final String property;
    private final Sort.Direction direction;

    ProductSortType(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public Sort getSort() {
        return Sort.by(direction, property);
    }
}
