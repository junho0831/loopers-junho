package com.loopers.batch.ranking.support;

public class ScoreUtil {
    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.2;
    private static final double ORDER_WEIGHT = 0.7;

    public record Aggregated(Long productId, Long likes, Long orders, Long views) {
        public double score() {
            return (VIEW_WEIGHT * views) + (LIKE_WEIGHT * likes) + (ORDER_WEIGHT * orders);
        }
    }
}
