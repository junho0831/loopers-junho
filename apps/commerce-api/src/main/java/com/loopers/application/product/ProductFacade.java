package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.*;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.interfaces.api.ProductDetailResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.data.domain.Page;
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

    public ProductFacade(JpaProductRepository productRepository, BrandService brandService, ProductService productService) {
        this.productRepository = productRepository;
        this.brandService = brandService;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        // N+1 문제 해결: 브랜드 정보와 함께 한 번에 조회
        Product product = productRepository.findByIdWithBrand(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));
        
        ProductDetail productDetail = productService.createProductDetail(product, product.getBrand());
        return ProductDetailResponse.from(productDetail);
    }

    public Page<Product> getProducts(ProductSortType sortType, Long brandId, Pageable pageable) {
        // 동적 정렬 적용
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            sortType.getSort()
        );
        
        if (brandId != null) {
            return productRepository.findByBrandIdOrderBy(brandId, sortedPageable);
        } else {
            return productRepository.findAll(sortedPageable);
        }
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