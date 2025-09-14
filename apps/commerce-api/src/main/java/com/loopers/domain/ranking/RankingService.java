package com.loopers.domain.ranking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.connection.RedisZSetCommands;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis ZSET을 활용한 실시간 랭킹 시스템 서비스
 * 상품 랭킹의 집계, 조회, 관리를 담당합니다
 */
@Service
public class RankingService {
    
    private static final Logger log = LoggerFactory.getLogger(RankingService.class);
    private static final Duration TTL = Duration.ofDays(2); // TTL: 2일
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RankingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    // 주의: 쓰기 작업은 Streamer 모듈에서 수행합니다. API 모듈은 조회 전용입니다.
    
    /**
     * Top-N 랭킹 조회 (ZSetOperations 사용)
     */
    public List<RankingItem> getTopRanking(RankingKey rankingKey, int size, int page) {
        try {
            String key = rankingKey.getKey();
            long start = (long) page * size;
            long end = start + size - 1;
            
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            Set<ZSetOperations.TypedTuple<Object>> tuples = zSetOps.reverseRangeWithScores(key, start, end);
            
            List<RankingItem> items = new ArrayList<>();
            long rank = start + 1; // 1부터 시작하는 순위
            
            if (tuples != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                    String memberStr = tuple.getValue().toString();
                    Long productId = Long.valueOf(memberStr);
                    Double score = tuple.getScore();
                    
                    items.add(new RankingItem(productId, score, rank++));
                }
            }
            
            return items;
                
        } catch (Exception e) {
            log.error("Failed to get top ranking for key={}", rankingKey.getKey(), e);
            throw e;
        }
    }
    
    /**
     * 특정 상품의 랭킹 정보 조회 (ZSetOperations 사용)
     */
    public RankingItem getProductRanking(RankingKey rankingKey, Long productId) {
        try {
            String key = rankingKey.getKey();
            String member = productId.toString();
            
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            
            Double score = zSetOps.score(key, member);
            if (score == null) {
                return null; // 랭킹에 없는 상품
            }
            
            Long rank = zSetOps.reverseRank(key, member);
            if (rank == null) {
                return null;
            }
            
            return new RankingItem(productId, score, rank + 1); // 1부터 시작
            
        } catch (Exception e) {
            log.error("Failed to get product ranking for productId={} in key={}", 
                productId, rankingKey.getKey(), e);
            throw e;
        }
    }
    
    /**
     * 랭킹 전체 상품 수 조회 (ZSetOperations 사용)
     */
    public long getRankingSize(RankingKey rankingKey) {
        try {
            String key = rankingKey.getKey();
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            Long size = zSetOps.zCard(key);
            return size != null ? size : 0L;
            
        } catch (Exception e) {
            log.error("Failed to get ranking size for key={}", rankingKey.getKey(), e);
            throw e;
        }
    }
    
    /**
     * 콜드 스타트 문제 해결을 위한 전날 점수 이월 (ZUNIONSTORE 최적화)
     */
    public void carryOverPreviousRanking(RankingKey currentKey, RankingKey previousKey) {
        try {
            String currentKeyStr = currentKey.getKey();
            String previousKeyStr = previousKey.getKey();
            
            // 이전 키가 존재하는지 확인
            if (Boolean.FALSE.equals(redisTemplate.hasKey(previousKeyStr))) {
                log.info("Previous ranking key does not exist: {}", previousKeyStr);
                return;
            }
            
            // 현재 키가 이미 존재하는지 확인 (중복 실행 방지)
            if (Boolean.TRUE.equals(redisTemplate.hasKey(currentKeyStr))) {
                log.info("Current ranking key already exists: {}", currentKeyStr);
                return;
            }
            
            // ZUNIONSTORE로 한 번에 가중치 적용하여 복사 (ZSetOperations.unionAndStore 사용)
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            
            // Spring Data Redis의 unionAndStore는 가중치를 지원하지 않으므로
            // 직접 RedisTemplate execute로 Redis 명령어 실행
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                byte[] currentKeyBytes = currentKeyStr.getBytes(StandardCharsets.UTF_8);
                byte[] previousKeyBytes = previousKeyStr.getBytes(StandardCharsets.UTF_8);
                
                // Raw Redis ZUNIONSTORE 명령어 실행
                // ZUNIONSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]]
                connection.execute("ZUNIONSTORE", 
                    currentKeyBytes,
                    "1".getBytes(StandardCharsets.UTF_8),
                    previousKeyBytes,
                    "WEIGHTS".getBytes(StandardCharsets.UTF_8),
                    String.valueOf(RankingWeight.CARRY_OVER_WEIGHT).getBytes(StandardCharsets.UTF_8));
                
                // TTL 설정 (한 번만)
                connection.expire(currentKeyBytes, TTL.getSeconds());
                
                return null;
            });
            
            long carriedCount = getRankingSize(currentKey);
            log.info("Carried over {} products from {} to {} with ZUNIONSTORE weight {}", 
                carriedCount, previousKeyStr, currentKeyStr, RankingWeight.CARRY_OVER_WEIGHT);
                
        } catch (Exception e) {
            log.error("Failed to carry over ranking from {} to {}", 
                previousKey.getKey(), currentKey.getKey(), e);
            throw e;
        }
    }
    
    /**
     * 랭킹 데이터 삭제
     */
    public void deleteRanking(RankingKey rankingKey) {
        try {
            redisTemplate.delete(rankingKey.getKey());
            log.info("Deleted ranking key: {}", rankingKey.getKey());
        } catch (Exception e) {
            log.error("Failed to delete ranking key: {}", rankingKey.getKey(), e);
            throw e;
        }
    }
    
    /**
     * 배치로 여러 상품의 점수를 업데이트 (Pipeline + Connection API 최적화)
     */
    public void batchIncrementScores(RankingKey rankingKey, List<ProductScore> productScores) {
        if (productScores == null || productScores.isEmpty()) {
            return;
        }
        
        try {
            String key = rankingKey.getKey();
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            
            // Pipeline으로 배치 처리 (RTT 최소화)
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (ProductScore productScore : productScores) {
                    if (productScore == null) continue;
                    Long pid = productScore.productId();
                    double s = productScore.score();
                    if (pid == null) continue;
                    if (Double.isNaN(s) || Double.isInfinite(s) || s == 0.0) continue;
                    byte[] memberBytes = pid.toString().getBytes(StandardCharsets.UTF_8);
                    connection.zIncrBy(keyBytes, s, memberBytes);
                }
                return null;
            });
            
            // TTL 설정 (한 번만)
            setTTLIfNeeded(key);
            
            log.info("Batch updated {} product scores in key={} with pipeline", productScores.size(), key);
            
        } catch (Exception e) {
            log.error("Failed to batch update scores in key={}", rankingKey.getKey(), e);
            throw e;
        }
    }
    
    /**
     * TTL 설정 헬퍼 메소드 (중복 설정 방지 최적화)
     */
    private void setTTLIfNeeded(String key) {
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
     * 상품 점수 정보를 담는 레코드
     */
    public record ProductScore(Long productId, double score) {}
}
