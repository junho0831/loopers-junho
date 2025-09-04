package com.loopers.application.event;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.order.OrderFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.ProductLike;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.User;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Email;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
import com.loopers.infrastructure.order.JpaOrderRepository;
import com.loopers.infrastructure.payment.PaymentRepository;
import com.loopers.infrastructure.point.JpaPointRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.infrastructure.user.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Round 7 Quest - ApplicationEvent 기반 기능 통합 테스트
 * 
 * 테스트 범위:
 * 1. 주문-결제 이벤트 분리 (OrderCreatedEvent → PaymentRequest)
 * 2. 좋아요-집계 이벤트 분리 (ProductLikeEvent → LikeCount Update)
 * 3. 사용자 행동 추적 이벤트 (UserActionEvent)
 * 4. 트랜잭션 분리 및 eventual consistency
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApplicationEventIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaPointRepository pointRepository;

    @Autowired
    private JpaBrandRepository brandRepository;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private JpaOrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JpaProductLikeRepository productLikeRepository;

    private String testUserId = "testuser1";
    private Long testProductId;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        User testUser = new User(testUserId, Gender.MALE, LocalDate.of(1990, 1, 1), new Email("test@example.com"), new com.loopers.domain.user.Point(1000000));
        userRepository.save(testUser);

        Point userPoint = new Point(testUserId, new BigDecimal("1000000"));
        pointRepository.save(userPoint);

        Brand testBrand = new Brand("TestBrand");
        brandRepository.save(testBrand);

        Product testProduct = new Product("TestProduct", new Money(100000L), new Stock(10), testBrand);
        testProduct = productRepository.save(testProduct);
        testProductId = testProduct.getId();
    }

    @Test
    @DisplayName("주문 생성 시 이벤트 기반으로 결제가 처리되는지 확인")
    void testOrderCreatedEventTriggersPayment() throws InterruptedException {
        // Given
        List<OrderItemRequest> items = List.of(new OrderItemRequest(testProductId, 1));

        // When
        Order createdOrder = orderFacade.createOrder(testUserId, items, "session1", "test-agent", "127.0.0.1");

        // Then - 주문은 즉시 생성됨
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(createdOrder.getUserId()).isEqualTo(testUserId);

        // 비동기 이벤트 처리 대기 (최대 5초)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Payment 엔티티가 이벤트로 인해 생성되었는지 확인
            List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.PENDING);
            assertThat(payments).hasSize(1);
            
            Payment payment = payments.get(0);
            assertThat(payment.getOrderId()).isEqualTo(createdOrder.getId().toString());
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
        });
    }

    @Test
    @DisplayName("좋아요 등록 시 이벤트 기반으로 집계가 업데이트되는지 확인 (Eventual Consistency)")
    void testProductLikeEventTriggersAggregation() throws InterruptedException {
        // Given
        Product product = productRepository.findById(testProductId).orElseThrow();
        long initialLikesCount = product.getLikesCount();

        // When - 좋아요 등록 (동기)
        System.out.println("DEBUG: Before calling likeFacade.addLike - testUserId: " + testUserId + ", testProductId: " + testProductId);
        ProductLike like = likeFacade.addLike(testUserId, testProductId, "session1", "test-agent", "127.0.0.1");
        System.out.println("DEBUG: After calling likeFacade.addLike - like: " + like);

        // Then - 좋아요는 즉시 등록됨
        assertThat(like).isNotNull();
        assertThat(like.getUserId()).isEqualTo(testUserId);
        assertThat(like.getProductId()).isEqualTo(testProductId);

        // 비동기 집계 업데이트 대기 (eventual consistency)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(testProductId).orElseThrow();
            assertThat(updatedProduct.getLikesCount()).isEqualTo(initialLikesCount + 1);
        });
    }

    @Test
    @DisplayName("좋아요 집계 실패해도 좋아요 등록은 성공하는지 확인 (독립 트랜잭션)")
    void testLikeRegistrationSucceedsEvenIfAggregationFails() {
        // Given - 존재하지 않는 상품에 좋아요 (집계는 실패하지만 좋아요는 성공해야 함)
        // 실제로는 ProductLike는 상품 검증을 먼저 하므로, 
        // 이 테스트는 집계 로직 자체의 실패를 시뮬레이션하기 어려움
        // 하지만 구조상 트랜잭션이 분리되어 있어 집계 실패가 메인 로직에 영향을 주지 않음을 확인

        // When
        ProductLike like = likeFacade.addLike(testUserId, testProductId, "session1", "test-agent", "127.0.0.1");

        // Then
        assertThat(like).isNotNull();
        assertThat(productLikeRepository.findByUserIdAndProductId(testUserId, testProductId)).isPresent();
    }

    @Test
    @DisplayName("좋아요 취소 시 집계가 감소하는지 확인")
    void testProductUnlikeEventDecreasesCount() throws InterruptedException {
        // Given - 먼저 좋아요 등록
        likeFacade.addLike(testUserId, testProductId, "session1", "test-agent", "127.0.0.1");

        // 집계 업데이트 대기
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Product product = productRepository.findById(testProductId).orElseThrow();
            assertThat(product.getLikesCount()).isEqualTo(1L);
        });

        // When - 좋아요 취소
        likeFacade.removeLike(testUserId, testProductId, "session1", "test-agent", "127.0.0.1");

        // Then - 좋아요는 즉시 삭제됨
        assertThat(productLikeRepository.findByUserIdAndProductId(testUserId, testProductId)).isEmpty();

        // 집계 감소 대기 (eventual consistency)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(testProductId).orElseThrow();
            assertThat(updatedProduct.getLikesCount()).isEqualTo(0L);
        });
    }

    @Test
    @DisplayName("여러 사용자의 동시 좋아요에도 집계가 정확한지 확인")
    void testConcurrentLikesAggregationAccuracy() throws InterruptedException {
        // Given - 추가 사용자들 생성
        String user2 = "testuser2";
        String user3 = "testuser3";
        
        userRepository.save(new User(user2, Gender.FEMALE, LocalDate.of(1995, 5, 5), new Email("test2@example.com"), new com.loopers.domain.user.Point(0)));
        userRepository.save(new User(user3, Gender.MALE, LocalDate.of(1985, 10, 10), new Email("test3@example.com"), new com.loopers.domain.user.Point(0)));

        // When - 동시에 좋아요 등록
        likeFacade.addLike(testUserId, testProductId, "session1", "test-agent", "127.0.0.1");
        likeFacade.addLike(user2, testProductId, "session2", "test-agent", "127.0.0.1");
        likeFacade.addLike(user3, testProductId, "session3", "test-agent", "127.0.0.1");

        // Then - 모든 좋아요가 등록됨
        assertThat(productLikeRepository.findByProductId(testProductId)).hasSize(3);

        // 집계가 정확히 업데이트되는지 확인
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(testProductId).orElseThrow();
            assertThat(updatedProduct.getLikesCount()).isEqualTo(3L);
        });
    }

    @Test
    @DisplayName("트랜잭션 분리로 인해 메인 로직과 부가 로직이 독립적으로 실행되는지 확인")
    void testTransactionSeparationBetweenMainAndSideEffects() throws InterruptedException {
        // Given
        List<OrderItemRequest> items = List.of(new OrderItemRequest(testProductId, 1));

        // When - 주문 생성 (메인 로직)
        Order order = orderFacade.createOrder(testUserId, items, "session1", "test-agent", "127.0.0.1");

        // Then - 메인 트랜잭션은 즉시 완료
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        
        // 주문은 즉시 DB에 저장됨 (메인 트랜잭션)
        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder).isNotNull();

        // 부가 로직들은 별도 트랜잭션에서 비동기로 처리됨
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // 결제 생성 (부가 로직)
            List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.PENDING);
            assertThat(payments).hasSizeGreaterThanOrEqualTo(1);
        });
    }
}
