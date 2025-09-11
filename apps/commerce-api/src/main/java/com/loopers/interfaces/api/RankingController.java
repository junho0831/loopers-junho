package com.loopers.interfaces.api;

import com.loopers.application.ranking.RankingFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 랭킹 API 컨트롤러
 * Redis ZSET 기반 실시간 랭킹 시스템의 조회 API를 제공합니다
 */
@RestController
@RequestMapping("/api/v1/rankings")
@Tag(name = "Ranking API", description = "상품 랭킹 조회 API")
public class RankingController {
    
    private static final Logger log = LoggerFactory.getLogger(RankingController.class);
    
    private final RankingFacade rankingFacade;
    
    public RankingController(RankingFacade rankingFacade) {
        this.rankingFacade = rankingFacade;
    }
    
    /**
     * 일간 랭킹 조회 API
     */
    @GetMapping
    @Operation(summary = "일간 상품 랭킹 조회", description = "특정 날짜의 상품 랭킹을 페이징으로 조회합니다")
    public ResponseEntity<ApiResponse<Page<RankingResponse>>> getDailyRanking(
            @Parameter(description = "조회할 날짜 (yyyyMMdd 또는 YYYY-MM-DD)", example = "20250908")
            @RequestParam(required = false) String date,
            
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") 
            int size,
            
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") 
            int page
    ) {
        try {
            // 날짜 파싱 (yyyyMMdd 또는 ISO yyyy-MM-dd 지원)
            LocalDate queryDate = parseDateOrToday(date);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<RankingResponse> rankings = rankingFacade.getDailyRanking(queryDate, pageable);
            
            log.info("Retrieved daily ranking for date: {}, page: {}, size: {}, total: {}", 
                queryDate, page, size, rankings.getTotalElements());
            
            return ResponseEntity.ok(ApiResponse.success(rankings));
            
        } catch (Exception e) {
            log.error("Failed to get daily ranking", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(ApiResponse.Metadata.fail("RANKING_ERROR", "Failed to retrieve ranking data"), null));
        }
    }
    
    /**
     * 오늘의 랭킹 조회 API (간편 버전)
     */
    @GetMapping("/today")
    @Operation(summary = "오늘의 상품 랭킹 조회", description = "오늘의 상품 랭킹을 페이징으로 조회합니다")
    public ResponseEntity<ApiResponse<Page<RankingResponse>>> getTodayRanking(
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<RankingResponse> rankings = rankingFacade.getTodayRanking(pageable);
            
            log.info("Retrieved today's ranking, page: {}, size: {}, total: {}", 
                page, size, rankings.getTotalElements());
            
            return ResponseEntity.ok(ApiResponse.success(rankings));
            
        } catch (Exception e) {
            log.error("Failed to get today's ranking", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(ApiResponse.Metadata.fail("RANKING_ERROR", "Failed to retrieve today's ranking data"), null));
        }
    }
    
    /**
     * Top-N 랭킹 조회 API
     */
    @GetMapping("/top/{count}")
    @Operation(summary = "Top-N 상품 랭킹 조회", description = "상위 N개 상품의 랭킹을 조회합니다")
    public ResponseEntity<ApiResponse<Page<RankingResponse>>> getTopRanking(
            @Parameter(description = "조회할 상위 개수", example = "10")
            @PathVariable int count,
            
            @Parameter(description = "조회할 날짜 (yyyyMMdd 또는 YYYY-MM-DD)", example = "20250908")
            @RequestParam(required = false) String date
    ) {
        try {
            if (count <= 0 || count > 100) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(ApiResponse.Metadata.fail("INVALID_COUNT", "Count must be between 1 and 100"), null));
            }
            
            LocalDate queryDate = parseDateOrToday(date);
            Pageable pageable = PageRequest.of(0, count);
            
            Page<RankingResponse> rankings = rankingFacade.getDailyRanking(queryDate, pageable);
            
            log.info("Retrieved top {} ranking for date: {}, actual size: {}", 
                count, queryDate, rankings.getContent().size());
            
            return ResponseEntity.ok(ApiResponse.success(rankings));
            
        } catch (Exception e) {
            log.error("Failed to get top {} ranking", count, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(ApiResponse.Metadata.fail("RANKING_ERROR", "Failed to retrieve top ranking data"), null));
        }
    }
    
    /**
     * 특정 상품의 랭킹 정보 조회 API
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "상품별 랭킹 정보 조회", description = "특정 상품의 랭킹 정보를 조회합니다")
    public ResponseEntity<ApiResponse<RankingResponse>> getProductRanking(
            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId,
            
            @Parameter(description = "조회할 날짜 (yyyyMMdd 또는 YYYY-MM-DD)", example = "20250908")
            @RequestParam(required = false) String date
    ) {
        try {
            LocalDate queryDate = parseDateOrToday(date);
            RankingResponse ranking = rankingFacade.getProductRanking(productId, queryDate);
            
            log.info("Retrieved product ranking for productId: {}, date: {}, rank: {}", 
                productId, queryDate, ranking.getRank());
            
            return ResponseEntity.ok(ApiResponse.success(ranking));
            
        } catch (IllegalArgumentException e) {
            log.warn("Product not found: {}", productId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get product ranking for productId: {}", productId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(ApiResponse.Metadata.fail("RANKING_ERROR", "Failed to retrieve product ranking"), null));
        }
    }
    
    /**
     * 랭킹 통계 정보 조회 API
     */
    @GetMapping("/stats")
    @Operation(summary = "랭킹 통계 조회", description = "특정 날짜의 랭킹 통계 정보를 조회합니다")
    public ResponseEntity<ApiResponse<RankingFacade.RankingStats>> getRankingStats(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD 형식)", example = "2025-09-08")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate date
    ) {
        try {
            LocalDate queryDate = date != null ? date : LocalDate.now();
            RankingFacade.RankingStats stats = rankingFacade.getRankingStats(queryDate);
            
            log.info("Retrieved ranking stats for date: {}, total products: {}", 
                queryDate, stats.totalProducts());
            
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            log.error("Failed to get ranking stats", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(ApiResponse.Metadata.fail("RANKING_ERROR", "Failed to retrieve ranking statistics"), null));
}

    // yyyyMMdd 또는 yyyy-MM-dd를 파싱, 미지정 시 오늘 날짜 반환
    private LocalDate parseDateOrToday(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            if (dateStr.matches("\\d{8}")) {
                return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            return LocalDate.parse(dateStr); // ISO yyyy-MM-dd
        } catch (Exception e) {
            // 형식이 잘못된 경우 오늘 날짜 기본값 사용
            return LocalDate.now();
        }
    }
}
}
