package com.loopers.domain.user;

public enum UserActionType {
    // 상품 관련
    PRODUCT_VIEW("상품 목록 조회"),
    PRODUCT_DETAIL("상품 상세 조회"),
    PRODUCT_LIKE("상품 좋아요"),
    PRODUCT_UNLIKE("상품 좋아요 취소"),
    
    // 주문 관련
    ORDER_CREATED("주문 생성"),
    ORDER_CANCELLED("주문 취소");

    private final String description;
    
    UserActionType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
