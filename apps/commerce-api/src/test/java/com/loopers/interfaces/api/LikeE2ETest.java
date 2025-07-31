package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
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

import com.loopers.application.like.LikeFacade;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LikeE2ETest {

    private static final String ENDPOINT_ADD_LIKE = "/api/v1/like/products/{productId}";
    private static final String ENDPOINT_REMOVE_LIKE = "/api/v1/like/products/{productId}";
    private static final String ENDPOINT_GET_LIKED_PRODUCTS = "/api/v1/like/products";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @MockitoBean
    private LikeFacade likeFacade;

    @MockitoBean
    private JpaProductRepository productRepository;

    private String testUserId = "testUser";
    private Long testProductId = 1L;

    @BeforeEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/like/products/{productId}")
    @Nested
    class AddLike {

        @DisplayName("좋아요 추가에 성공할 경우, 200 OK 응답을 반환한다")
        @Test
        void addLike_WithValidRequest_ReturnsOk() {
            // given
            when(productRepository.existsById(testProductId)).thenReturn(true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_ADD_LIKE.replace("{productId}", testProductId.toString()),
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("User-Id 헤더가 없을 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void addLike_WithoutHeader_ReturnsBadRequest() {
            // given
            HttpEntity<Void> httpEntity = new HttpEntity<>(new HttpHeaders());

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_ADD_LIKE.replace("{productId}", testProductId.toString()),
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 상품에 좋아요를 추가할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void addLike_WithNonExistentProduct_ReturnsNotFound() {
            // given
            Long nonExistentProductId = 999L;
            when(productRepository.existsById(nonExistentProductId)).thenReturn(false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_ADD_LIKE.replace("{productId}", nonExistentProductId.toString()),
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api/v1/like/products/{productId}")
    @Nested
    class RemoveLike {

        @DisplayName("좋아요 취소에 성공할 경우, 200 OK 응답을 반환한다")
        @Test
        void removeLike_WithValidRequest_ReturnsOk() {
            // given
            when(productRepository.existsById(testProductId)).thenReturn(true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_REMOVE_LIKE.replace("{productId}", testProductId.toString()),
                    HttpMethod.DELETE,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("User-Id 헤더가 없을 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void removeLike_WithoutHeader_ReturnsBadRequest() {
            // given
            HttpEntity<Void> httpEntity = new HttpEntity<>(new HttpHeaders());

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_REMOVE_LIKE.replace("{productId}", testProductId.toString()),
                    HttpMethod.DELETE,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("GET /api/v1/like/products")
    @Nested
    class GetLikedProducts {

        @DisplayName("좋아요한 상품 목록 조회에 성공할 경우, 상품 목록을 응답으로 반환한다")
        @Test
        void getLikedProducts_WithValidRequest_ReturnsProductList() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_LIKED_PRODUCTS,
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("User-Id 헤더가 없을 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void getLikedProducts_WithoutHeader_ReturnsBadRequest() {
            // given
            HttpEntity<Void> httpEntity = new HttpEntity<>(new HttpHeaders());

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_LIKED_PRODUCTS,
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}