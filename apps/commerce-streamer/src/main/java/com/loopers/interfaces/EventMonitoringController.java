package com.loopers.interfaces;

import com.loopers.domain.event.EventLog;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.service.EventTestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 이벤트 처리 모니터링을 위한 REST 컨트롤러
 */
@RestController
@RequestMapping("/api/events")
public class EventMonitoringController {
    
    private final EventTestService eventTestService;

    public EventMonitoringController(EventTestService eventTestService) {
        this.eventTestService = eventTestService;
    }

    /**
     * 이벤트 처리 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<EventTestService.EventProcessingStats> getProcessingStats() {
        EventTestService.EventProcessingStats stats = eventTestService.getProcessingStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 특정 이벤트가 처리되었는지 확인
     */
    @GetMapping("/verify/{eventId}")
    public ResponseEntity<EventVerificationResponse> verifyEventProcessing(@PathVariable String eventId) {
        boolean processed = eventTestService.verifyEventProcessing(eventId);
        EventVerificationResponse response = new EventVerificationResponse();
        response.setEventId(eventId);
        response.setProcessed(processed);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 날짜의 제품 메트릭 조회
     */
    @GetMapping("/metrics/product/{productId}")
    public ResponseEntity<ProductMetrics> getProductMetrics(
            @PathVariable Long productId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate queryDate = date != null ? date : LocalDate.now();
        ProductMetrics metrics = eventTestService.getProductMetrics(productId, queryDate);
        
        if (metrics != null) {
            return ResponseEntity.ok(metrics);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 제품에 대한 Redis 캐시 키 조회
     */
    @GetMapping("/cache/product/{productId}")
    public ResponseEntity<Set<String>> getProductCacheKeys(@PathVariable Long productId) {
        Set<String> cacheKeys = eventTestService.getProductCacheKeys(productId);
        return ResponseEntity.ok(cacheKeys);
    }
    
    /**
     * 감사 로그에서 최근 이벤트 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<List<EventLog>> getRecentEvents(
            @RequestParam(defaultValue = "10") int limit) {
        List<EventLog> recentEvents = eventTestService.getRecentEvents(limit);
        return ResponseEntity.ok(recentEvents);
    }
    
    /**
     * 헬스 체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Event processing system is healthy");
    }
    
    /**
     * 이벤트 검증을 위한 응답 클래스
     */
    public static class EventVerificationResponse {
        private String eventId;
        private boolean processed;
        
        // Getter와 Setter 메서드들
        public String getEventId() {
            return eventId;
        }
        
        public void setEventId(String eventId) {
            this.eventId = eventId;
        }
        
        public boolean isProcessed() {
            return processed;
        }
        
        public void setProcessed(boolean processed) {
            this.processed = processed;
        }
    }
}