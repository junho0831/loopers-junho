package com.loopers.domain.ranking;

/**
 * 랭킹 점수 계산을 위한 가중치 관리 클래스
 * 좋아요, 주문, 조회 등의 이벤트에 따른 가중치를 관리합니다
 */
public class RankingWeight {
    
    // 기본 가중치 설정
    public static final double VIEW_WEIGHT = 0.1;      // 조회: 가장 빈번하지만 낮은 가중치
    public static final double LIKE_WEIGHT = 0.2;      // 좋아요: 구매 의도보다는 낮은 가중치
    public static final double ORDER_WEIGHT = 0.7;     // 주문: 실제 구매 결정이므로 높은 가중치
    
    // 콜드 스타트 해결을 위한 이월 가중치
    public static final double CARRY_OVER_WEIGHT = 0.1; // 전날 점수의 10%만 이월
    
    /**
     * 이벤트 타입에 따른 가중치 점수 계산
     */
    public static double calculateScore(String eventType, long baseScore) {
        return switch (eventType.toUpperCase()) {
            case "PRODUCT_VIEWED" -> VIEW_WEIGHT * baseScore;
            case "PRODUCT_LIKED" -> LIKE_WEIGHT * baseScore;
            case "PRODUCT_UNLIKED" -> -LIKE_WEIGHT * baseScore; // 좋아요 취소는 음수
            case "ORDER_CREATED" -> ORDER_WEIGHT * baseScore;
            default -> 0.0;
        };
    }
    
    /**
     * 상품 메트릭 기반 종합 점수 계산
     * 가중치 합산: W(view)*count(view) + W(like)*count(like) + W(order)*count(order)
     */
    public static double calculateWeightedScore(long viewCount, long likeCount, long orderCount) {
        return (VIEW_WEIGHT * viewCount) + (LIKE_WEIGHT * likeCount) + (ORDER_WEIGHT * orderCount);
    }
    
    /**
     * 주문 이벤트의 경우 금액이나 수량 기반으로 점수 계산
     * 단순 횟수가 아닌 실제 비즈니스 가치 반영
     */
    public static double calculateOrderScore(long quantity, double unitPrice) {
        // 로그 스케일 적용으로 큰 주문의 영향력 조절
        double rawValue = quantity * unitPrice;
        return Math.log1p(rawValue) * ORDER_WEIGHT; // log(1 + value)로 스케일 조절
    }
    
    // 실시간 가중치 조절을 위한 확장 포인트
    private static class RuntimeWeights {
        private double viewWeight = VIEW_WEIGHT;
        private double likeWeight = LIKE_WEIGHT;
        private double orderWeight = ORDER_WEIGHT;
        
        // 향후 동적 가중치 조절 기능 확장 가능
    }
}