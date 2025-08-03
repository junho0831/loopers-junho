package com.loopers.interfaces.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Money;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderE2ETest {

    private static final String ENDPOINT_CREATE_ORDER = "/api/v1/orders";
    private static final String ENDPOINT_GET_ORDER = "/api/v1/orders/{orderId}";
    private static final String ENDPOINT_GET_USER_ORDERS = "/api/v1/orders";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @MockitoBean
    private OrderFacade orderFacade;

    private String testUserId = "testUser";
    private Long testProductId = 1L;
    private Long testOrderId = 1L;

    @BeforeEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    class CreateOrder {

        @DisplayName("주문 생성에 성공할 경우, 생성된 주문 정보를 응답으로 반환한다")
        @Test
        void createOrder_WithValidRequest_ReturnsOrderInfo() {
            // given
            Order mockOrder = createMockOrder();
            when(orderFacade.createOrder(anyString(), any())).thenReturn(mockOrder);
            
            Map<String, Object> requestBody = Map.of(
                    "items", List.of(Map.of(
                            "productId", testProductId,
                            "quantity", 2)));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_CREATE_ORDER,
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
        void createOrder_WithoutHeader_ReturnsBadRequest() {
            // given
            Map<String, Object> requestBody = Map.of(
                    "items", List.of(Map.of(
                            "productId", testProductId,
                            "quantity", 2)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_CREATE_ORDER,
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 상품으로 주문할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void createOrder_WithNonExistentProduct_ReturnsNotFound() {
            // given
            Long nonExistentProductId = 999L;
            when(orderFacade.createOrder(anyString(), any()))
                    .thenThrow(new IllegalArgumentException("Product not found: " + nonExistentProductId));
            
            Map<String, Object> requestBody = Map.of(
                    "items", List.of(Map.of(
                            "productId", nonExistentProductId,
                            "quantity", 2)));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_CREATE_ORDER,
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("재고가 부족한 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void createOrder_WithInsufficientStock_ReturnsBadRequest() {
            // given
            when(orderFacade.createOrder(anyString(), any()))
                    .thenThrow(new IllegalArgumentException("Insufficient stock for product: Test Product"));
            
            Map<String, Object> requestBody = Map.of(
                    "items", List.of(Map.of(
                            "productId", testProductId,
                            "quantity", 15 // 재고보다 많은 수량
                    )));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_CREATE_ORDER,
                    HttpMethod.POST,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    class GetOrder {

        @DisplayName("주문 조회에 성공할 경우, 주문 정보를 응답으로 반환한다")
        @Test
        void getOrder_WithValidOrderId_ReturnsOrderInfo() {
            // given
            Order mockOrder = createMockOrder();
            when(orderFacade.getOrderById(testOrderId)).thenReturn(Optional.of(mockOrder));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_ORDER.replace("{orderId}", testOrderId.toString()),
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull());
        }

        @DisplayName("존재하지 않는 주문 ID로 조회할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void getOrder_WithNonExistentOrderId_ReturnsNotFound() {
            // given
            Long nonExistentOrderId = 999L;
            when(orderFacade.getOrderById(nonExistentOrderId)).thenReturn(Optional.empty());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_ORDER.replace("{orderId}", nonExistentOrderId.toString()),
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    class GetUserOrders {

        @DisplayName("사용자 주문 목록 조회에 성공할 경우, 주문 목록을 응답으로 반환한다")
        @Test
        void getUserOrders_WithValidRequest_ReturnsOrderList() {
            // given
            Order mockOrder = createMockOrder();
            when(orderFacade.getUserOrders(testUserId)).thenReturn(List.of(mockOrder));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_USER_ORDERS,
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
        void getUserOrders_WithoutHeader_ReturnsBadRequest() {
            // given
            HttpEntity<Void> httpEntity = new HttpEntity<>(new HttpHeaders());

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_USER_ORDERS,
                    HttpMethod.GET,
                    httpEntity,
                    String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    private Order createMockOrder() {
        // Mock Order 객체 생성
        Order order = new Order(testUserId, List.of(), new Money(10000L));
        // Reflection을 사용하여 id 설정
        try {
            java.lang.reflect.Field idField = order.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, testOrderId);
        } catch (Exception e) {
            // Reflection 실패 시 무시
        }
        return order;
    }
}