package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Stock;
import com.loopers.domain.product.ProductSortType;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
import com.loopers.domain.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.loopers.domain.BaseEntity;

@ExtendWith(MockitoExtension.class)
class ProductServiceIntegrationTest {

    @Mock
    private JpaProductRepository productRepository;

    @Mock
    private JpaBrandRepository brandRepository;

    @Mock
    private JpaProductLikeRepository likeRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductFacade productFacade;

    private Brand testBrand;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testBrand = new Brand("테스트 브랜드");
        testProduct1 = new Product("상품1", new Money(10000), new Stock(10), testBrand);
        testProduct2 = new Product("상품2", new Money(20000), new Stock(5), testBrand);
        
        // 테스트용 ID 설정 (실제로는 데이터베이스에서 자동 생성됨)
        setEntityId(testBrand, 1L);
        setEntityId(testProduct1, 1L);
        setEntityId(testProduct2, 2L);
    }

    // 테스트용 ID 설정 헬퍼 메서드
    private void setEntityId(BaseEntity entity, Long id) {
        try {
            java.lang.reflect.Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID for test entity", e);
        }
    }

    @Test
    @DisplayName("상품 상세 조회에 성공할 경우, 상품과 브랜드 정보를 함께 반환한다")
    void getProductDetail_WithValidProductId_ReturnsProductDetail() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
        when(brandRepository.findById(testBrand.getId())).thenReturn(Optional.of(testBrand));
        when(productService.createProductDetail(any(), any())).thenReturn(new com.loopers.domain.product.ProductDetail(testProduct1, testBrand));

        // when
        com.loopers.interfaces.api.ProductDetailResponse result = productFacade.getProductDetail(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("상품1");
        assertThat(result.getBrandId()).isEqualTo(testBrand.getId());
        assertThat(result.getBrandName()).isEqualTo("테스트 브랜드");
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회할 경우, 예외가 발생한다")
    void getProductDetail_WithNonExistentProductId_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productFacade.getProductDetail(nonExistentProductId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("브랜드 ID로 필터링하여 상품 목록을 조회할 수 있다")
    void getProducts_WithBrandId_ReturnsFilteredProducts() {
        // given
        Long brandId = testBrand.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> mockPage = new PageImpl<>(List.of(testProduct1, testProduct2), pageable, 2);
        when(productRepository.findByBrandId(brandId, pageable)).thenReturn(mockPage);

        // when
        Page<Product> result = productFacade.getProducts(ProductSortType.LATEST, brandId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(product -> product.getBrandId().equals(brandId));
    }

    @Test
    @DisplayName("정렬 조건으로 상품 목록을 조회할 수 있다")
    void getProducts_WithSortType_ReturnsSortedProducts() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> mockPage = new PageImpl<>(List.of(testProduct1, testProduct2), pageable, 2);
        when(productRepository.findAllOrderBy(ProductSortType.PRICE_ASC, pageable)).thenReturn(mockPage);

        // when
        Page<Product> result = productFacade.getProducts(ProductSortType.PRICE_ASC, null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        // 가격 오름차순 정렬 확인
        assertThat(result.getContent().get(0).getPrice().getValue())
                .isLessThanOrEqualTo(result.getContent().get(1).getPrice().getValue());
    }

    @Test
    @DisplayName("상품을 생성할 수 있다")
    void createProduct_WithValidData_ReturnsCreatedProduct() {
        // given
        String name = "새 상품";
        long price = 15000;
        int stock = 20;
        Long brandId = testBrand.getId();
        
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = productFacade.createProduct(name, price, stock, brandId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getPrice().getValue()).isEqualTo(price);
        assertThat(result.getStock().getQuantity()).isEqualTo(stock);
        assertThat(result.getBrandId()).isEqualTo(brandId);
    }

    @Test
    @DisplayName("존재하지 않는 브랜드 ID로 상품 생성 시 예외가 발생한다")
    void createProduct_WithNonExistentBrandId_ThrowsException() {
        // given
        String name = "새 상품";
        long price = 15000;
        int stock = 20;
        Long nonExistentBrandId = 999L;
        when(brandRepository.findById(nonExistentBrandId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productFacade.createProduct(name, price, stock, nonExistentBrandId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Brand not found");
    }

    @Test
    @DisplayName("상품 재고를 차감할 수 있다")
    void decreaseProductStock_WithValidQuantity_DecreasesStock() {
        // given
        Long productId = 1L;
        int quantity = 3;
        int originalStock = testProduct1.getStock().getQuantity();
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        productFacade.decreaseProductStock(productId, quantity);

        // then
        assertThat(testProduct1.getStock().getQuantity()).isEqualTo(originalStock - quantity);
    }

    @Test
    @DisplayName("재고보다 많은 수량으로 차감 시도 시 예외가 발생한다")
    void decreaseProductStock_WithInsufficientStock_ThrowsException() {
        // given
        Long productId = 1L;
        int quantity = 15; // 재고보다 많은 수량
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        // when & then
        assertThatThrownBy(() -> productFacade.decreaseProductStock(productId, quantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }
}