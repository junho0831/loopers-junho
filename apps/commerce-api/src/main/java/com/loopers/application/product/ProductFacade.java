package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.*;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.interfaces.api.ProductDetailResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductFacade {
    private final JpaProductRepository productRepository;
    private final BrandService brandService;
    private final ProductService productService;
    private final ProductCacheService cacheService;

    public ProductFacade(JpaProductRepository productRepository, BrandService brandService, 
                        ProductService productService, ProductCacheService cacheService) {
        this.productRepository = productRepository;
        this.brandService = brandService;
        this.productService = productService;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        // 1. 캐시에서 먼저 조회
        ProductDetailResponse cachedResponse = cacheService.getProductDetail(productId);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        
        // 2. 캐시 미스 시 DB에서 조회 (PK 인덱스 활용)
        // N+1 문제 해결: 브랜드 정보와 함께 한 번에 조회
        Product product = productRepository.findByIdWithBrand(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
        
        ProductDetail productDetail = productService.createProductDetail(product, product.getBrand());
        ProductDetailResponse response = ProductDetailResponse.from(productDetail);
        
        // 3. 응답을 캐시에 저장
        cacheService.cacheProductDetail(productId, response);
        
        return response;
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
            return productRepository.findByBrandId(brandId, sortedPageable);
        } else {
            return productRepository.findAll(sortedPageable);
        }
    }
    
    // ID로 단일 상품 조회 (PK 인덱스 최적화)
    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        return productRepository.findByProductId(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
    }

    public Product createProduct(String name, long price, int stock, Long brandId) {
        productService.validateProductCreation(name, price, stock);
        
        Brand brand = brandService.loadBrand(brandId);
        Product product = new Product(name, new Money(price), new Stock(stock), brand);
        return productRepository.save(product);
    }

    public void decreaseProductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
        product.decreaseStock(quantity);
        productRepository.save(product);
    }
}