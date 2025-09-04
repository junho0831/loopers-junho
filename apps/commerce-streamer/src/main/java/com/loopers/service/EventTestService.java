package com.loopers.service;

import com.loopers.domain.event.EventLog;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.repository.EventLogRepository;
import com.loopers.repository.ProductMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 이벤트 파이프라인 테스트 및 모니터링을 위한 서비스
 */
@Service
public class EventTestService {
    
    private static final Logger log = LoggerFactory.getLogger(EventTestService.class);
    
    private final EventLogRepository eventLogRepository;
    private final ProductMetricsRepository productMetricsRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public EventTestService(EventLogRepository eventLogRepository,
                           ProductMetricsRepository productMetricsRepository,
                           RedisTemplate<String, Object> redisTemplate) {
        this.eventLogRepository = eventLogRepository;
        this.productMetricsRepository = productMetricsRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 모든 컴슈머에 의해 이벤트가 처리되었는지 확인
     */
    public boolean verifyEventProcessing(String eventId) {
        log.info("Verifying event processing for eventId: {}", eventId);
        
        // 이벤트가 감사 로그에 기록되었는지 확인
        boolean auditLogged = eventLogRepository.existsByEventId(eventId);
        log.info("Event {} audit logged: {}", eventId, auditLogged);
        
        return auditLogged;
    }
    
    /**
     * 이벤트 처리 통계 조회
     */
    public EventProcessingStats getProcessingStats() {
        long totalEvents = eventLogRepository.count();
        long totalMetrics = productMetricsRepository.count();
        
        EventProcessingStats stats = new EventProcessingStats();
        stats.setTotalEventsProcessed(totalEvents);
        stats.setTotalMetricsRecords(totalMetrics);
        
        log.info("Event processing stats - Events: {}, Metrics: {}", totalEvents, totalMetrics);
        
        return stats;
    }
    
    /**
     * 특정 제품과 날짜에 대한 메트릭 조회
     */
    public ProductMetrics getProductMetrics(Long productId, LocalDate date) {
        return productMetricsRepository.findByProductIdAndMetricDate(productId, date);
    }
    
    /**
     * 제품에 대한 Redis 캐시 키 확인
     */
    public Set<String> getProductCacheKeys(Long productId) {
        String pattern = "*product:" + productId + "*";
        return redisTemplate.keys(pattern);
    }
    
    /**
     * 감사 로그에서 최근 이벤트 조회
     */
    public List<EventLog> getRecentEvents(int limit) {
        List<EventLog> allEvents = eventLogRepository.findAll();
        return allEvents.stream()
                .sorted((a, b) -> b.getProcessedAt().compareTo(a.getProcessedAt()))
                .limit(limit)
                .toList();
    }
    
    /**
     * 이벤트 처리를 위한 통계 클래스
     */
    public static class EventProcessingStats {
        private long totalEventsProcessed;
        private long totalMetricsRecords;
        
        // Getter와 Setter 메서드들
        public long getTotalEventsProcessed() {
            return totalEventsProcessed;
        }
        
        public void setTotalEventsProcessed(long totalEventsProcessed) {
            this.totalEventsProcessed = totalEventsProcessed;
        }
        
        public long getTotalMetricsRecords() {
            return totalMetricsRecords;
        }
        
        public void setTotalMetricsRecords(long totalMetricsRecords) {
            this.totalMetricsRecords = totalMetricsRecords;
        }
    }
}