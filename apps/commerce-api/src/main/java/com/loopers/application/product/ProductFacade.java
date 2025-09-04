package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.*;
import com.loopers.domain.user.UserActionEvent;
import com.loopers.interfaces.api.ProductDetailResponse;
import com.loopers.interfaces.api.ProductController.ProductListResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductFacade {
    private final BrandService brandService;
    private final ProductService productService;
    private final ProductCacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;

    public ProductFacade(BrandService brandService, 
                        ProductService productService, ProductCacheService cacheService,
                        ApplicationEventPublisher eventPublisher) {
        this.brandService = brandService;
        this.productService = productService;
        this.cacheService = cacheService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        // 사용자 행동 추적 없는 기본 메서드 (테스트용)
        // 1. 캐시에서 먼저 조회
        ProductDetailResponse cachedResponse = cacheService.getProductDetail(productId);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        
        // 2. 캐시 미스 시 DB에서 조회 (PK 인덱스 활용)
        // N+1 문제 해결: 브랜드 정보와 함께 한 번에 조회
        Product product = productService.findByIdWithBrand(productId);
        
        ProductDetail productDetail = productService.createProductDetail(product, product.getBrand());
        ProductDetailResponse response = ProductDetailResponse.from(productDetail);
        
        // 3. 응답을 캐시에 저장
        cacheService.cacheProductDetail(productId, response);
        
        return response;
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId, String userId, String sessionId, String userAgent, String ipAddress) {
        // 1. 사용자 행동 추적: 상품 상세 조회
        publishUserActionEvent(UserActionEvent.productDetail(userId, productId, sessionId, userAgent, ipAddress));
        
        // 2. 기본 메서드 호출
        return getProductDetail(productId);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProducts(ProductSortType sortType, Long brandId, Pageable pageable) {
        // 동적 정렬 적용
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            sortType.getSort()
        );
        
        // 인덱스를 활용한 최적화된 쿼리 실행
        if (brandId != null) {
            return productService.findByBrandId(brandId, sortedPageable);
        } else {
            return productService.findAll(sortedPageable);
        }
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getProductsWithTracking(ProductSortType sortType, Long brandId, Pageable pageable, String userId, String sessionId, String userAgent, String ipAddress) {
        // 1. 사용자 행동 추적: 상품 목록 조회
        publishUserActionEvent(UserActionEvent.productView(userId, 0L, sessionId, userAgent, ipAddress));
        
        // 2. 기존 로직 호출
        return getProducts(sortType, brandId, pageable);
    }
    
    // ID로 단일 상품 조회 (기본키 인덱스 최적화)
    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        return productService.findByProductId(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
    }

    public Product createProduct(String name, long price, int stock, Long brandId) {
        productService.validateProductCreation(name, price, stock);
        
        Brand brand = brandService.loadBrand(brandId);
        Product product = new Product(name, new Money(price), new Stock(stock), brand);
        return productService.saveProduct(product);
    }

    public void decreaseProductStock(Long productId, int quantity) {
        Product product = productService.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
        product.decreaseStock(quantity);
        productService.saveProduct(product);
        
        // 카프카 파이프라인을 위한 재고 조정 이벤트 발행
        try {
            eventPublisher.publishEvent(new StockAdjustedEvent(productId, -quantity));
        } catch (Exception e) {
            // 로그만 남기고 주요 작업은 실패시키지 않음
        }
    }
    
    /**
     * 카프카 파이프라인용 재고 조정 이벤트
     */
    public static class StockAdjustedEvent {
        private final Long productId;
        private final int quantityChanged;
        
        public StockAdjustedEvent(Long productId, int quantityChanged) {
            this.productId = productId;
            this.quantityChanged = quantityChanged;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public int getQuantityChanged() {
            return quantityChanged;
        }
    }
    
    // === API 전용 메서드들 (컨트롤러에서 호출) ===
    
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProductsForApi(Long brandId, String sort, int page, int size, String userId, HttpServletRequest request) {
        // 1. 파라미터 검증 및 변환
        ProductSortType sortType;
        try {
            sortType = ProductSortType.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_SORT_TYPE);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        // 2. 사용자 행동 추적 정보 추출
        String sessionId = request.getSession().getId();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        
        // 3. 비즈니스 로직 실행
        Page<Product> products = getProductsWithTracking(sortType, brandId, pageable, userId, sessionId, userAgent, ipAddress);
        
        // 4. null 체크
        if (products == null) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }
        
        // 5. 응답 변환
        return products.map(ProductListResponse::new);
    }
    
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailForApi(Long productId, String userId, HttpServletRequest request) {
        // 1. 사용자 행동 추적 정보 추출
        String sessionId = request.getSession().getId();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        
        // 2. 비즈니스 로직 실행
        return getProductDetail(productId, userId, sessionId, userAgent, ipAddress);
    }
    
    // 도우미 메서드: IP 주소 추출
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    // 도우미 메서드: 안전한 이벤트 발행
    private void publishUserActionEvent(UserActionEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            // 추적 실패해도 주요 비즈니스 로직에는 영향 없음
        }
    }
}