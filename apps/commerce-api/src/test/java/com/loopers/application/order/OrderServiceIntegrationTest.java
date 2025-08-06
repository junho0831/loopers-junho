package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Stock;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;

import java.math.BigDecimal;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.infrastructure.order.JpaOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class OrderServiceIntegrationTest {

    @Mock
    private JpaOrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PointService pointService;

    @Mock
    private OrderService orderService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderFacade orderFacade;

    private Brand testBrand;
    private Product testProduct1;
    private Product testProduct2;
    private Point testUserPoint;
    private String testUserId = "testUser";

    @BeforeEach
    void setUp() {
        testBrand = new Brand("테스트 브랜드");
        testProduct1 = new Product("상품1", new Money(10000), new Stock(10), testBrand);
        testProduct2 = new Product("상품2", new Money(20000), new Stock(5), testBrand);
        testUserPoint = new Point(testUserId, BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("정상적인 주문 생성에 성공할 경우, Order 객체를 반환한다")
    void createOrder_WithValidRequest_ReturnsOrder() {
        // given
        Long productId1 = 1L;
        Long productId2 = 2L;
        List<OrderItemRequest> itemRequests = List.of(
                new OrderItemRequest(productId1, 2),
                new OrderItemRequest(productId2, 1));

        when(productService.loadProductsWithLock(itemRequests)).thenReturn(List.of(testProduct1, testProduct2));
        when(pointService.loadUserPointsWithLock(testUserId)).thenReturn(testUserPoint);
        when(orderService.calculateTotalAmount(any(), any())).thenReturn(new Money(40000));
        when(orderService.createOrderItems(any(), any())).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenReturn(new Order(testUserId, List.of(), new Money(40000)));

        // when
        Order result = orderFacade.createOrder(testUserId, itemRequests);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문할 경우, 예외가 발생한다")
    void createOrder_WithNonExistentProduct_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;
        List<OrderItemRequest> itemRequests = List.of(
                new OrderItemRequest(nonExistentProductId, 1));

        when(productService.loadProductsWithLock(itemRequests))
                .thenThrow(new IllegalArgumentException("Product not found: " + nonExistentProductId));

        // when & then
        assertThatThrownBy(() -> orderFacade.createOrder(testUserId, itemRequests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("재고가 부족한 경우, 예외가 발생한다")
    void createOrder_WithInsufficientStock_ThrowsException() {
        // given
        Long productId = 1L;
        List<OrderItemRequest> itemRequests = List.of(
                new OrderItemRequest(productId, 15) // 재고보다 많은 수량
        );

        when(productService.loadProductsWithLock(itemRequests)).thenReturn(List.of(testProduct1));
        doThrow(new IllegalArgumentException("Insufficient stock")).when(orderService).validateProductsStock(any(), any());

        // when & then
        assertThatThrownBy(() -> orderFacade.createOrder(testUserId, itemRequests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("포인트가 부족한 경우, 예외가 발생한다")
    void createOrder_WithInsufficientPoints_ThrowsException() {
        // given
        Long productId = 1L;
        Point insufficientPoint = new Point(testUserId, BigDecimal.valueOf(5000)); // 포인트 부족
        
        List<OrderItemRequest> itemRequests = List.of(
                new OrderItemRequest(productId, 1) // 10000원 상품
        );

        when(productService.loadProductsWithLock(itemRequests)).thenReturn(List.of(testProduct1));
        when(pointService.loadUserPointsWithLock(testUserId)).thenReturn(insufficientPoint);
        when(orderService.calculateTotalAmount(any(), any())).thenReturn(new Money(10000));
        doThrow(new IllegalArgumentException("Insufficient points balance.")).when(pointService).validateUserPoints(any(), any());

        // when & then
        assertThatThrownBy(() -> orderFacade.createOrder(testUserId, itemRequests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient points");
    }

    @Test
    @DisplayName("사용자의 주문 목록을 조회할 수 있다")
    void getUserOrders_WithValidUserId_ReturnsOrderList() {
        // given
        List<Order> mockOrders = List.of(
                new Order(testUserId, List.of(), new Money(10000)),
                new Order(testUserId, List.of(), new Money(20000))
        );
        
        when(orderRepository.findByUserId(testUserId)).thenReturn(mockOrders);

        // when
        List<Order> result = orderFacade.getUserOrders(testUserId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(order -> order.getUserId().equals(testUserId));
    }

    @Test
    @DisplayName("주문이 없는 사용자의 주문 목록 조회 시 빈 리스트를 반환한다")
    void getUserOrders_WithNoOrders_ReturnsEmptyList() {
        // given
        String userWithNoOrders = "userWithNoOrders";

        // when
        List<Order> result = orderFacade.getUserOrders(userWithNoOrders);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주문 ID로 주문을 조회할 수 있다")
    void getOrderById_WithValidOrderId_ReturnsOrder() {
        // given
        Long orderId = 1L;
        Order mockOrder = new Order(testUserId, List.of(), new Money(10000));
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // when
        var result = orderFacade.getOrderById(orderId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 조회할 경우, 빈 Optional을 반환한다")
    void getOrderById_WithNonExistentOrderId_ReturnsEmpty() {
        // given
        Long nonExistentOrderId = 999L;

        // when
        var result = orderFacade.getOrderById(nonExistentOrderId);

        // then
        assertThat(result).isEmpty();
    }
}
