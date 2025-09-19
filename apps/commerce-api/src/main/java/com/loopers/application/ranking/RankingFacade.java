package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingService;
import com.loopers.interfaces.api.RankingResponse;
import com.loopers.infrastructure.ranking.MvProductRankMonthlyRepository;
import com.loopers.infrastructure.ranking.MvProductRankWeeklyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 랭킹 관련 비즈니스 로직을 처리하는 Facade
 * 랭킹 데이터와 상품 데이터를 조합하여 완전한 응답을 제공합니다
 */
@Service
public class RankingFacade {
    
    private static final Logger log = LoggerFactory.getLogger(RankingFacade.class);
    
    private final RankingService rankingService;
    private final ProductService productService;
    private final MvProductRankWeeklyRepository weeklyRepo;
    private final MvProductRankMonthlyRepository monthlyRepo;
    
    public RankingFacade(RankingService rankingService,
                         ProductService productService,
                         MvProductRankWeeklyRepository weeklyRepo,
                         MvProductRankMonthlyRepository monthlyRepo) {
        this.rankingService = rankingService;
        this.productService = productService;
        this.weeklyRepo = weeklyRepo;
        this.monthlyRepo = monthlyRepo;
    }
    
    /**
     * 일간 랭킹 조회 (페이징)
     */
    public Page<RankingResponse> getDailyRanking(LocalDate date, Pageable pageable) {
        try {
            RankingKey rankingKey = RankingKey.dailyAll(date);
            
            // Redis에서 랭킹 데이터 조회
            List<RankingItem> rankingItems = rankingService.getTopRanking(
                rankingKey, 
                pageable.getPageSize(), 
                pageable.getPageNumber()
            );
            
            if (rankingItems.isEmpty()) {
                log.info("No ranking data found for date: {}", date);
                return Page.empty(pageable);
            }
            
            // 상품 ID 목록 추출
            Set<Long> productIds = rankingItems.stream()
                .map(RankingItem::getProductId)
                .collect(Collectors.toSet());
            
            // 상품 정보 조회 (배치)
            List<Product> products = productService.findAllByIds(productIds);
            Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
            
            // 랭킹 응답 생성
            List<RankingResponse> responses = rankingItems.stream()
                .filter(item -> productMap.containsKey(item.getProductId()))
                .map(item -> new RankingResponse(productMap.get(item.getProductId()), item))
                .toList();
            
            // 전체 랭킹 수 조회
            long totalElements = rankingService.getRankingSize(rankingKey);
            
            log.info("Retrieved {} ranking items for date: {} (page: {}, size: {})", 
                responses.size(), date, pageable.getPageNumber(), pageable.getPageSize());
            
            return new PageImpl<>(responses, pageable, totalElements);
            
        } catch (Exception e) {
            log.error("Failed to get daily ranking for date: {}", date, e);
            throw new RuntimeException("Failed to retrieve ranking data", e);
        }
    }

    public enum Period { DAILY, WEEKLY, MONTHLY }

    public Page<RankingResponse> getRanking(LocalDate date, Period period, Pageable pageable) {
        return switch (period) {
            case DAILY -> getDailyRanking(date, pageable);
            case WEEKLY -> getWeeklyRanking(date, pageable);
            case MONTHLY -> getMonthlyRanking(date, pageable);
        };
    }

    public Page<RankingResponse> getWeeklyRanking(LocalDate date, Pageable pageable) {
        try {
            String yearWeek = YearWeekUtil.toYearWeek(date);
            var page = weeklyRepo.findByYearWeekOrderByRankAsc(yearWeek, pageable);
            if (page.isEmpty()) return Page.empty(pageable);

            Set<Long> productIds = page.getContent().stream().map(MvProductRankWeekly::getProductId).collect(Collectors.toSet());
            Map<Long, Product> productMap = productService.findAllByIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            List<RankingResponse> responses = page.getContent().stream()
                    .filter(r -> productMap.containsKey(r.getProductId()))
                    .map(r -> new RankingResponse(productMap.get(r.getProductId()), new RankingItem(r.getProductId(), r.getScore(), r.getRank().longValue())))
                    .toList();

            long total = weeklyRepo.countByYearWeek(yearWeek);
            return new PageImpl<>(responses, pageable, total);
        } catch (Exception e) {
            log.error("Failed to get weekly ranking for date: {}", date, e);
            throw new RuntimeException("Failed to retrieve weekly ranking", e);
        }
    }

    public Page<RankingResponse> getMonthlyRanking(LocalDate date, Pageable pageable) {
        try {
            String yearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyyMM").format(date);
            var page = monthlyRepo.findByYearMonthOrderByRankAsc(yearMonth, pageable);
            if (page.isEmpty()) return Page.empty(pageable);

            Set<Long> productIds = page.getContent().stream().map(MvProductRankMonthly::getProductId).collect(Collectors.toSet());
            Map<Long, Product> productMap = productService.findAllByIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            List<RankingResponse> responses = page.getContent().stream()
                    .filter(r -> productMap.containsKey(r.getProductId()))
                    .map(r -> new RankingResponse(productMap.get(r.getProductId()), new RankingItem(r.getProductId(), r.getScore(), r.getRank().longValue())))
                    .toList();

            long total = monthlyRepo.countByYearMonth(yearMonth);
            return new PageImpl<>(responses, pageable, total);
        } catch (Exception e) {
            log.error("Failed to get monthly ranking for date: {}", date, e);
            throw new RuntimeException("Failed to retrieve monthly ranking", e);
        }
    }
    
    /**
     * 오늘의 랭킹 조회
     */
    public Page<RankingResponse> getTodayRanking(Pageable pageable) {
        return getDailyRanking(LocalDate.now(), pageable);
    }
    
    /**
     * 특정 상품의 랭킹 정보 조회
     */
    public RankingResponse getProductRanking(Long productId, LocalDate date) {
        try {
            RankingKey rankingKey = RankingKey.dailyAll(date);
            
            // 상품 정보 조회
            Product product = productService.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            
            // 랭킹 정보 조회
            RankingItem rankingItem = rankingService.getProductRanking(rankingKey, productId);
            
            if (rankingItem == null) {
                log.debug("Product {} is not in ranking for date: {}", productId, date);
                return new RankingResponse(product);
            }
            
            return new RankingResponse(product, rankingItem);
            
        } catch (Exception e) {
            log.error("Failed to get product ranking for productId: {}, date: {}", productId, date, e);
            throw new RuntimeException("Failed to retrieve product ranking", e);
        }
    }

    public RankingResponse getProductRanking(Long productId, LocalDate date, Period period) {
        return switch (period) {
            case DAILY -> getProductRanking(productId, date);
            case WEEKLY -> getProductWeeklyRanking(productId, date);
            case MONTHLY -> getProductMonthlyRanking(productId, date);
        };
    }

    public RankingResponse getProductWeeklyRanking(Long productId, LocalDate date) {
        String yearWeek = YearWeekUtil.toYearWeek(date);
        Product product = productService.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return weeklyRepo.findByYearWeekAndProductId(yearWeek, productId)
                .map(r -> new RankingResponse(product, new RankingItem(productId, r.getScore(), r.getRank().longValue())))
                .orElseGet(() -> new RankingResponse(product));
    }

    public RankingResponse getProductMonthlyRanking(Long productId, LocalDate date) {
        String yearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyyMM").format(date);
        Product product = productService.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return monthlyRepo.findByYearMonthAndProductId(yearMonth, productId)
                .map(r -> new RankingResponse(product, new RankingItem(productId, r.getScore(), r.getRank().longValue())))
                .orElseGet(() -> new RankingResponse(product));
    }
    
    /**
     * 오늘 특정 상품의 랭킹 정보 조회
     */
    public RankingResponse getProductTodayRanking(Long productId) {
        return getProductRanking(productId, LocalDate.now());
    }
    
    /**
     * 랭킹 통계 정보 조회
     */
    public RankingStats getRankingStats(LocalDate date) {
        try {
            RankingKey rankingKey = RankingKey.dailyAll(date);
            long totalProducts = rankingService.getRankingSize(rankingKey);
            
            // Top 10 조회로 상위권 통계 계산
            List<RankingItem> top10 = rankingService.getTopRanking(rankingKey, 10, 0);
            
            double averageTopScore = top10.stream()
                .mapToDouble(RankingItem::getScore)
                .average()
                .orElse(0.0);
            
            double maxScore = top10.stream()
                .mapToDouble(RankingItem::getScore)
                .max()
                .orElse(0.0);
            
            return new RankingStats(date, totalProducts, maxScore, averageTopScore);
            
        } catch (Exception e) {
            log.error("Failed to get ranking stats for date: {}", date, e);
            throw new RuntimeException("Failed to retrieve ranking statistics", e);
        }
    }
    
    /**
     * 랭킹 통계 정보
     */
    public record RankingStats(
        LocalDate date,
        long totalProducts,
        double maxScore,
        double averageTopScore
    ) {}

    // Helper for ISO week key
    static class YearWeekUtil {
        static String toYearWeek(LocalDate date) {
            var weekFields = java.time.temporal.WeekFields.ISO;
            int week = date.get(weekFields.weekOfWeekBasedYear());
            int weekYear = date.get(weekFields.weekBasedYear());
            return String.format("%dW%02d", weekYear, week);
        }
    }
}
