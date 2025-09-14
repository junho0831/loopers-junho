package com.loopers.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis ZSET을 활용한 실시간 랭킹 집계 서비스 (Streamer용)
 * Kafka Consumer에서 이벤트를 소비해 랭킹 점수를 실시간으로 업데이트
 *
 * ZSET Key 전략
 * - Key: {@code ranking:all:yyyyMMdd}
 * - TTL: 2 days (172800s) — 최초 생성 시 1회만 설정
 */
@Service
public class RankingService {
    
    private static final Logger log = LoggerFactory.getLogger(RankingService.class);
    
    // 랭킹 키 설정
    private static final String RANKING_PREFIX = "ranking:all"; // final key = ranking:all:yyyyMMdd
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration TTL = Duration.ofDays(2);
    
    // 가중치 설정
    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.2;
    private static final double ORDER_WEIGHT = 0.7;
    private static final double CARRY_OVER_WEIGHT = 0.1;
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RankingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 이벤트 타입에 따른 랭킹 점수 업데이트
     */
    public void updateRankingScore(String eventType, Long productId, Long baseScore, LocalDate eventDate) {
        double score = calculateEventScore(eventType, baseScore);
        if (score == 0.0) {
            return; // 점수가 0이면 업데이트하지 않음
        }
        
        String rankingKey = generateRankingKey(eventDate);
        incrementScore(rankingKey, productId, score);
    }
    
    /**
     * 주문 이벤트의 특별 처리 (금액과 수량 고려)
     */
    public void updateOrderRankingScore(Long productId, Long quantity, Double unitPrice, LocalDate eventDate) {
        // 로그 스케일 적용으로 큰 주문의 영향력 조절
        double rawValue = quantity * (unitPrice != null ? unitPrice : 1.0);
        double score = Math.log1p(rawValue) * ORDER_WEIGHT;
        
        String rankingKey = generateRankingKey(eventDate);
        incrementScore(rankingKey, productId, score);
    }
    
    /**
     * 배치로 여러 상품의 점수를 업데이트
     */
    public void batchUpdateRankingScores(List<ProductScoreUpdate> updates, LocalDate eventDate) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        
        String rankingKey = generateRankingKey(eventDate);
        
        try {
            // Redis Pipeline을 사용하여 배치 처리 성능 향상
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] keyBytes = rankingKey.getBytes(StandardCharsets.UTF_8);
                
                for (ProductScoreUpdate update : updates) {
                    if (update == null) continue;
                    Long pid = update.productId();
                    if (pid == null) continue;
                    double score = calculateEventScore(update.eventType(), update.baseScore());
                    if (Double.isNaN(score) || Double.isInfinite(score) || score == 0.0) continue;
                    byte[] memberBytes = pid.toString().getBytes(StandardCharsets.UTF_8);
                    connection.zIncrBy(keyBytes, score, memberBytes);
                }
                return null;
            });
            
            // TTL 설정
            ensureTTL(rankingKey);
            
            log.info("Batch updated {} ranking scores for key={}", updates.size(), generateRankingKey(eventDate));
            
        } catch (Exception e) {
            log.error("Failed to batch update ranking scores for date={}", eventDate, e);
            throw e;
        }
    }
    
    /**
     * 콜드 스타트 문제 해결을 위한 전날 점수 이월 (ZUNIONSTORE 최적화)
     */
    public void carryOverPreviousRanking(LocalDate currentDate) {
        String currentKey = generateRankingKey(currentDate);
        String previousKey = generateRankingKey(currentDate.minusDays(1));
        
        try {
            // 이전 키가 존재하는지 확인
            if (Boolean.FALSE.equals(redisTemplate.hasKey(previousKey))) {
                log.info("Previous ranking key does not exist: {}", previousKey);
                return;
            }
            
            // 현재 키가 이미 존재하는지 확인 (중복 실행 방지)
            if (Boolean.TRUE.equals(redisTemplate.hasKey(currentKey))) {
                log.info("Current ranking key already exists: {}", currentKey);
                return;
            }
            
            // ZUNIONSTORE로 한 번에 가중치 적용하여 복사
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                byte[] currentKeyBytes = currentKey.getBytes(StandardCharsets.UTF_8);
                byte[] previousKeyBytes = previousKey.getBytes(StandardCharsets.UTF_8);
                
                // Raw Redis ZUNIONSTORE 명령어 실행
                // ZUNIONSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]]
                connection.execute("ZUNIONSTORE", 
                    currentKeyBytes,
                    "1".getBytes(StandardCharsets.UTF_8),
                    previousKeyBytes,
                    "WEIGHTS".getBytes(StandardCharsets.UTF_8),
                    String.valueOf(CARRY_OVER_WEIGHT).getBytes(StandardCharsets.UTF_8));
                
                // TTL 설정 (한 번만)
                connection.expire(currentKeyBytes, TTL.getSeconds());
                
                return null;
            });
            
            Long carriedCount = redisTemplate.opsForZSet().zCard(currentKey);
            log.info("Carried over {} products from {} to {} with ZUNIONSTORE weight {}", 
                carriedCount, previousKey, currentKey, CARRY_OVER_WEIGHT);
                
        } catch (Exception e) {
            log.error("Failed to carry over ranking from {} to {}", previousKey, currentKey, e);
            throw e;
        }
    }
    
    private double calculateEventScore(String eventType, Long baseScore) {
        return switch (eventType.toUpperCase()) {
            case "PRODUCT_VIEWED" -> VIEW_WEIGHT * baseScore;
            case "PRODUCT_LIKED" -> LIKE_WEIGHT * baseScore;
            case "PRODUCT_UNLIKED" -> -LIKE_WEIGHT * baseScore; // 좋아요 취소는 음수
            case "ORDER_CREATED" -> ORDER_WEIGHT * baseScore;
            default -> 0.0;
        };
    }
    
    private void incrementScore(String rankingKey, Long productId, double score) {
        try {
            String member = productId.toString();
            
            Double newScore = redisTemplate.execute((RedisCallback<Double>) connection -> {
                byte[] keyBytes = rankingKey.getBytes(StandardCharsets.UTF_8);
                byte[] memberBytes = member.getBytes(StandardCharsets.UTF_8);
                
                // ZINCRBY key increment member
                return connection.zIncrBy(keyBytes, score, memberBytes);
            });
            
            ensureTTL(rankingKey);
            
            log.debug("Incremented score for productId={} in key={}, newScore={}", 
                productId, rankingKey, newScore);
                
        } catch (Exception e) {
            log.error("Failed to increment score for productId={} in key={}", 
                productId, rankingKey, e);
            throw e;
        }
    }
    
    private void ensureTTL(String key) {
        try {
            // TTL이 -1인 경우에만 설정 (이미 설정된 키는 스킵)
            Long ttl = redisTemplate.getExpire(key);
            if (ttl == -1) { // TTL이 설정되지 않은 경우만
                redisTemplate.expire(key, TTL);
                log.debug("Set TTL for ranking key: {}", key);
            } else if (ttl > 0) {
                log.trace("TTL already set for key: {}, remaining: {}s", key, ttl);
            }
        } catch (Exception e) {
            log.warn("Failed to set TTL for key: {}", key, e);
        }
    }
    
    /**
     * 일자별 랭킹 키 계산: ranking:all:yyyyMMdd
     */
    private String generateRankingKey(LocalDate date) {
        return String.format("%s:%s", RANKING_PREFIX, date.format(DATE_FORMAT));
    }

    /**
     * 외부에서 키 전략을 재사용할 수 있도록 공개 Helper 제공
     */
    public String getDailyKey(LocalDate date) {
        return generateRankingKey(date);
    }
    
    /**
     * 상품 점수 업데이트 정보를 담는 레코드
     */
    public record ProductScoreUpdate(Long productId, String eventType, Long baseScore) {}
}
