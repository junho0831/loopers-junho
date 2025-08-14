package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.interfaces.api.ProductDetailResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 상품 캐시 서비스
 * Redis를 이용한 상품 정보 캐싱 처리
 * - 상품 상세 정보 캐싱
 * - 상품 목록 캐싱
 * - 인기 상품 캐싱
 * - 캐시 무효화 관리
 */
@Service
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 캐시 키 접두사
    private static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_PREFIX = "product:list:";
    private static final String POPULAR_PRODUCTS_KEY = "product:popular";
    
    // TTL 설정
    private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(10);  // 상품 상세: 10분
    private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(5);     // 상품 목록: 5분
    private static final Duration POPULAR_PRODUCTS_TTL = Duration.ofMinutes(30); // 인기 상품: 30분

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 상품 상세 캐시 관리 메서드들
    /**
     * 캐시에서 상품 상세 정보 조회
     */
    public ProductDetailResponse getProductDetail(Long productId) {
        String key = PRODUCT_DETAIL_PREFIX + productId;
        return (ProductDetailResponse) redisTemplate.opsForValue().get(key);
    }

    /**
     * 상품 상세 정보 캐시에 저장 (TTL: 10분)
     */
    public void cacheProductDetail(Long productId, ProductDetailResponse productDetail) {
        String key = PRODUCT_DETAIL_PREFIX + productId;
        redisTemplate.opsForValue().set(key, productDetail, PRODUCT_DETAIL_TTL);
    }

    /**
     * 상품 상세 정보 캐시 삭제
     */
    public void evictProductDetail(Long productId) {
        String key = PRODUCT_DETAIL_PREFIX + productId;
        redisTemplate.delete(key);
    }

    // 상품 목록 캐시 관리 메서드들
    /**
     * 캐시에서 상품 목록 조회
     */
    public List<Product> getProductList(String cacheKey) {
        String key = PRODUCT_LIST_PREFIX + cacheKey;
        @SuppressWarnings("unchecked")
        List<Product> cachedList = (List<Product>) redisTemplate.opsForValue().get(key);
        return cachedList;
    }

    /**
     * 상품 목록 캐시에 저장 (TTL: 5분)
     */
    public void cacheProductList(String cacheKey, List<Product> products) {
        String key = PRODUCT_LIST_PREFIX + cacheKey;
        redisTemplate.opsForValue().set(key, products, PRODUCT_LIST_TTL);
    }

    /**
     * 상품 목록 캐시 삭제
     */
    public void evictProductList(String cacheKey) {
        String key = PRODUCT_LIST_PREFIX + cacheKey;
        redisTemplate.delete(key);
    }

    // 인기 상품 캐시 관리 메서드들
    /**
     * 캐시에서 인기 상품 목록 조회
     */
    public List<Product> getPopularProducts() {
        @SuppressWarnings("unchecked")
        List<Product> cachedProducts = (List<Product>) redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY);
        return cachedProducts;
    }

    /**
     * 인기 상품 목록 캐시에 저장 (TTL: 30분)
     */
    public void cachePopularProducts(List<Product> products) {
        redisTemplate.opsForValue().set(POPULAR_PRODUCTS_KEY, products, POPULAR_PRODUCTS_TTL);
    }

    /**
     * 인기 상품 목록 캐시 삭제
     */
    public void evictPopularProducts() {
        redisTemplate.delete(POPULAR_PRODUCTS_KEY);
    }

    // 상품 업데이트 시 관련 캐시 무효화
    /**
     * 상품 정보 변경 시 관련된 모든 캐시 삭제
     * - 상품 상세 캐시
     * - 인기 상품 캐시 (좋아요 수 변경 시)
     * - 상품 목록 캐시들
     */
    public void evictProductCaches(Long productId) {
        // 상품 상세 캐시 삭제
        evictProductDetail(productId);
        
        // 인기 상품 캐시 삭제 (해당 상품이 영향을 줄 수 있음)
        evictPopularProducts();
        
        // 상품 목록 캐시들 삭제 - 실제 운영에서는 더 세밀한 제어 가능
        evictProductListCachesByPattern();
    }

    /**
     * 패턴에 맞는 상품 목록 캐시들 모두 삭제
     */
    private void evictProductListCachesByPattern() {
        // 모든 상품 목록 캐시 삭제
        // 운영환경에서는 더 선별적으로 삭제할 수 있음
        redisTemplate.delete(redisTemplate.keys(PRODUCT_LIST_PREFIX + "*"));
    }

    // 상품 목록 쿼리용 캐시 키 생성
    /**
     * 상품 목록 조회 조건을 기반으로 캐시 키 생성
     * 정렬 타입, 브랜드 ID, 페이지 정보를 조합하여 유니크한 키 생성
     */
    public String generateProductListCacheKey(String sortType, Long brandId, int page, int size) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("sort:").append(sortType);
        
        if (brandId != null) {
            keyBuilder.append(":brand:").append(brandId);
        }
        
        keyBuilder.append(":page:").append(page).append(":size:").append(size);
        
        return keyBuilder.toString();
    }

    // 캐시 통계 (모니터링용)
    /**
     * 현재 저장된 캐시 키의 총 개수 반환
     * 간단한 캐시 사용량 모니터링용
     */
    public long getCacheSize() {
        // 운영환경에서는 더 상세한 메트릭이 필요할 수 있음
        return redisTemplate.keys("*").size();
    }

    // 헬스체크
    /**
     * Redis 연결 상태 확인
     */
    public boolean isRedisConnected() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}