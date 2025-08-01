package com.loopers.domain.order;

import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.domain.brand.Brand;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Mock
        private com.loopers.infrastructure.order.JpaOrderRepository orderRepository;

        @Mock
        private com.loopers.infrastructure.product.JpaProductRepository productRepository;

        @Mock
        private com.loopers.infrastructure.point.JpaPointRepository pointRepository;

        private OrderService orderService;

        @BeforeEach
        void setUp() {
                orderService = new OrderService(orderRepository, productRepository, pointRepository);
        }

        @Test
        @DisplayName("정상적인 주문을 생성할 수 있다")
        void createOrder() {
                // given
                String userId = "user1";
                Long productId = 1L;
                int quantity = 2;

                Brand brand = new Brand("테스트 브랜드");
                Product product = new Product("테스트 상품", new Money(1000), new Stock(10), brand);
                Point userPoints = new Point(userId, BigDecimal.valueOf(5000));

                OrderItemRequest itemRequest = new OrderItemRequest(productId, quantity);
                List<OrderItemRequest> itemRequests = List.of(itemRequest);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(pointRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userPoints));
                when(productRepository.save(any(Product.class)))
                                .thenReturn(product);
                when(pointRepository.save(any(Point.class)))
                                .thenReturn(userPoints);
                when(orderRepository.save(any(Order.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // when
                Order result = orderService.createOrder(userId, itemRequests);

                // then
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getOrderItems()).hasSize(1);
                assertThat(result.getTotalAmount().getValue()).isEqualTo(2000);
                assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

                // 재고가 차감되었는지 확인
                verify(productRepository).save(product);
                // 포인트가 차감되었는지 확인
                verify(pointRepository).save(userPoints);
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 주문하면 예외가 발생한다")
        void createOrderWithNonExistentProduct() {
                // given
                String userId = "user1";
                Long productId = 999L;
                OrderItemRequest itemRequest = new OrderItemRequest(productId, 1);
                List<OrderItemRequest> itemRequests = List.of(itemRequest);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> orderService.createOrder(userId, itemRequests))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("재고가 부족하면 주문 시 예외가 발생한다")
        void createOrderWithInsufficientStock() {
                // given
                String userId = "user1";
                Long productId = 1L;
                int quantity = 15; // 재고보다 많은 수량

                Brand brand = new Brand("테스트 브랜드");
                Product product = new Product("테스트 상품", new Money(1000), new Stock(10), brand);

                OrderItemRequest itemRequest = new OrderItemRequest(productId, quantity);
                List<OrderItemRequest> itemRequests = List.of(itemRequest);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));

                // when & then
                assertThatThrownBy(() -> orderService.createOrder(userId, itemRequests))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("포인트가 부족하면 주문 시 예외가 발생한다")
        void createOrderWithInsufficientPoints() {
                // given
                String userId = "user1";
                Long productId = 1L;
                int quantity = 5;

                Brand brand = new Brand("테스트 브랜드");
                Product product = new Product("테스트 상품", new Money(1000), new Stock(10), brand);
                Point userPoints = new Point(userId, BigDecimal.valueOf(1000)); // 부족한 포인트

                OrderItemRequest itemRequest = new OrderItemRequest(productId, quantity);
                List<OrderItemRequest> itemRequests = List.of(itemRequest);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(pointRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userPoints));

                // when & then
                assertThatThrownBy(() -> orderService.createOrder(userId, itemRequests))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Insufficient points");
        }

        @Test
        @DisplayName("사용자 포인트 계정이 없으면 예외가 발생한다")
        void createOrderWithNoPointAccount() {
                // given
                String userId = "user1";
                Long productId = 1L;

                Brand brand = new Brand("테스트 브랜드");
                Product product = new Product("테스트 상품", new Money(1000), new Stock(10), brand);

                OrderItemRequest itemRequest = new OrderItemRequest(productId, 1);
                List<OrderItemRequest> itemRequests = List.of(itemRequest);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(pointRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> orderService.createOrder(userId, itemRequests))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("User has no points account");
        }

        @Test
        @DisplayName("여러 상품을 주문할 수 있다")
        void createOrderWithMultipleProducts() {
                // given
                String userId = "user1";
                Long productId1 = 1L;
                Long productId2 = 2L;

                Brand brand = new Brand("테스트 브랜드");
                Product product1 = new Product("상품1", new Money(1000), new Stock(10), brand);
                Product product2 = new Product("상품2", new Money(2000), new Stock(5), brand);
                Point userPoints = new Point(userId, BigDecimal.valueOf(10000));

                OrderItemRequest itemRequest1 = new OrderItemRequest(productId1, 2);
                OrderItemRequest itemRequest2 = new OrderItemRequest(productId2, 1);
                List<OrderItemRequest> itemRequests = List.of(itemRequest1, itemRequest2);

                when(productRepository.findById(productId1))
                                .thenReturn(Optional.of(product1));
                when(productRepository.findById(productId2))
                                .thenReturn(Optional.of(product2));
                when(pointRepository.findByUserId(userId))
                                .thenReturn(Optional.of(userPoints));
                when(productRepository.save(any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(pointRepository.save(any(Point.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(orderRepository.save(any(Order.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // when
                Order result = orderService.createOrder(userId, itemRequests);

                // then
                assertThat(result.getOrderItems()).hasSize(2);
                assertThat(result.getTotalAmount().getValue()).isEqualTo(4000); // 1000*2 + 2000*1
        }

        @Test
        @DisplayName("주문 ID로 주문을 조회할 수 있다")
        void getOrderById() {
                // given
                Long orderId = 1L;
                Order expectedOrder = new Order("user1", List.of(), new Money(0));

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(expectedOrder));

                // when
                Optional<Order> result = orderService.getOrderById(orderId);

                // then
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(expectedOrder);
        }

        @Test
        @DisplayName("사용자의 주문 목록을 조회할 수 있다")
        void getUserOrders() {
                // given
                String userId = "user1";
                List<Order> expectedOrders = List.of(
                                new Order(userId, List.of(), new Money(1000)),
                                new Order(userId, List.of(), new Money(2000)));

                when(orderRepository.findByUserId(userId))
                                .thenReturn(expectedOrders);

                // when
                List<Order> result = orderService.getUserOrders(userId);

                // then
                assertThat(result).hasSize(2);
                assertThat(result).isEqualTo(expectedOrders);
        }
}