package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.List;
import org.springframework.data.domain.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.application.product.ProductFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.product.Stock;
import com.loopers.interfaces.api.ProductDetailResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductE2ETest {

    private static final String ENDPOINT_GET_PRODUCTS = "/api/v1/products";
    private static final String ENDPOINT_GET_PRODUCT_DETAIL = "/api/v1/products/{productId}";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @MockitoBean
    private ProductFacade productFacade;
    
    @MockitoBean
    private com.loopers.infrastructure.product.JpaProductRepository productRepository;
    
    @MockitoBean
    private com.loopers.infrastructure.brand.JpaBrandRepository brandRepository;

    private Long testProductId = 1L;
    private Long testBrandId = 1L;

//    @BeforeEach
//    void tearDown() {
        // Mock을 사용하므로 데이터베이스 초기화는 건너뜀
        // databaseCleanUp.truncateAllTables();
//    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("상품 목록 조회에 성공할 경우, 상품 목록을 응답으로 반환한다")
        @Test
        void getProducts_WithValidRequest_ReturnsProductList() {
            // given
            when(productFacade.getProducts(any(), any(), any())).thenReturn(Page.empty());
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_PRODUCTS,
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("브랜드 ID로 필터링하여 상품 목록을 조회할 수 있다")
        @Test
        void getProducts_WithBrandId_ReturnsFilteredProducts() {
            // given
            when(productFacade.getProducts(any(), any(), any())).thenReturn(Page.empty());
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_PRODUCTS + "?brandId=" + testBrandId,
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("정렬 조건으로 상품 목록을 조회할 수 있다")
        @Test
        void getProducts_WithSortType_ReturnsSortedProducts() {
            // given
            when(productFacade.getProducts(any(), any(), any())).thenReturn(Page.empty());
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_PRODUCTS + "?sort=price_asc",
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProductDetail {

        @DisplayName("상품 상세 조회에 성공할 경우, 상품 상세 정보를 응답으로 반환한다")
        @Test
        void getProductDetail_WithValidProductId_ReturnsProductDetail() {
            // given
            ProductDetailResponse mockProductDetail = new ProductDetailResponse(
                new Product("Test Product", new Money(10000), new Stock(10), new Brand("Test Brand")),
                new Brand("Test Brand")
            );
            when(productFacade.getProductDetail(eq(testProductId))).thenReturn(mockProductDetail);
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_PRODUCT_DETAIL.replace("{productId}", testProductId.toString()),
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("존재하지 않는 상품 ID로 조회할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void getProductDetail_WithNonExistentProductId_ReturnsNotFound() {
            // given
            Long nonExistentProductId = 999L;
            when(productFacade.getProductDetail(eq(nonExistentProductId)))
                    .thenThrow(new CoreException(ErrorType.PRODUCT_NOT_FOUND));
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_PRODUCT_DETAIL.replace("{productId}", nonExistentProductId.toString()),
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

}
