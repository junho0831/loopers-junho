package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.point.JpaPointRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private JpaPointRepository pointRepository;

    @Autowired
    private JpaBrandRepository brandRepository;

    private Brand testBrand;
    private Product testProduct;
    private final String testUserId = "concurrency-test-user";

    @BeforeEach
    void setUp() {
        // 테스트용 브랜드 생성
        testBrand = new Brand("Test Brand");
        brandRepository.save(testBrand);

        // 테스트용 상품 생성 (재고 10개)
        testProduct = new Product("Test Product", new Money(1000), new Stock(10), testBrand);
        productRepository.save(testProduct);

        // 테스트용 사용자 포인트 생성 (충분한 포인트)
        Point userPoint = new Point(testUserId, BigDecimal.valueOf(100000));
        pointRepository.save(userPoint);
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감되어야 한다")
    void concurrency_multipleUsersOrderSameProduct_stockShouldNotGoNegative() throws InterruptedException {
        // given
        int threadCount = 15; // 재고(10개)보다 많은 수의 동시 요청
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when - 동시에 15명이 1개씩 주문
        for (int i = 0; i < threadCount; i++) {
            final String userId = "user-" + i;
            // 각 사용자마다 충분한 포인트 생성
            Point userPoint = new Point(userId, BigDecimal.valueOf(10000));
            pointRepository.save(userPoint);

            executor.submit(() -> {
                try {
                    List<OrderItemRequest> itemRequests = List.of(
                            new OrderItemRequest(testProduct.getId(), 1)
                    );
                    orderFacade.createOrder(userId, itemRequests);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        
        System.out.println("성공한 주문: " + successCount.get());
        System.out.println("실패한 주문: " + failureCount.get());
        System.out.println("최종 재고: " + updatedProduct.getStock().getQuantity());

        // 재고는 0 이상이어야 함
        assertThat(updatedProduct.getStock().getQuantity()).isGreaterThanOrEqualTo(0);
        
        // 성공한 주문 수 + 남은 재고 = 초기 재고(10)
        assertThat(successCount.get() + updatedProduct.getStock().getQuantity()).isEqualTo(10);
        
        // 성공한 주문은 10개 이하여야 함
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
        
        // 실패한 주문이 있어야 함 (15개 요청 중 10개만 성공)
        assertThat(failureCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("동일한 유저가 여러 기기에서 동시에 주문에도, 포인트가 중복 차감되지 않아야 한다")
    void concurrency_sameUserMultipleOrders_pointsShouldBeDeductedCorrectly() throws InterruptedException {
        // given
        int threadCount = 5;
        int orderAmount = 1000; // 각 주문마다 1000원
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 사용자 포인트를 정확히 3000원으로 설정 (3번의 주문만 성공해야 함)
        Point userPoint = pointRepository.findByUserId(testUserId).orElseThrow();
        userPoint.deductPoints(userPoint.getPointBalance().subtract(BigDecimal.valueOf(3000)));
        pointRepository.save(userPoint);

        // when - 같은 사용자가 동시에 5번 주문 (각각 1000원)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    List<OrderItemRequest> itemRequests = List.of(
                            new OrderItemRequest(testProduct.getId(), 1) // 1000원 상품 1개
                    );
                    orderFacade.createOrder(testUserId, itemRequests);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        Point updatedUserPoint = pointRepository.findByUserId(testUserId).orElseThrow();
        
        System.out.println("성공한 주문: " + successCount.get());
        System.out.println("실패한 주문: " + failureCount.get());
        System.out.println("최종 포인트: " + updatedUserPoint.getPointBalance());

        // 성공한 주문은 3개 이하여야 함 (3000원으로 1000원 상품을 최대 3개까지만 구매 가능)
        assertThat(successCount.get()).isLessThanOrEqualTo(3);
        
        // 포인트는 정확히 차감되어야 함
        BigDecimal expectedRemainingPoints = BigDecimal.valueOf(3000 - (successCount.get() * orderAmount));
        assertThat(updatedUserPoint.getPointBalance()).isEqualByComparingTo(expectedRemainingPoints);
        
        // 실패한 주문이 있어야 함
        assertThat(failureCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("재고가 부족할 때 모든 요청이 실패해야 한다")
    void concurrency_insufficientStock_allOrdersShouldFail() throws InterruptedException {
        // given
        // 재고를 0으로 설정
        testProduct.decreaseStock(10);
        productRepository.save(testProduct);
        
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when - 재고가 0인 상품을 동시에 주문
        for (int i = 0; i < threadCount; i++) {
            final String userId = "user-" + i;
            Point userPoint = new Point(userId, BigDecimal.valueOf(10000));
            pointRepository.save(userPoint);

            executor.submit(() -> {
                try {
                    List<OrderItemRequest> itemRequests = List.of(
                            new OrderItemRequest(testProduct.getId(), 1)
                    );
                    orderFacade.createOrder(userId, itemRequests);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(0); // 모든 주문이 실패해야 함
        assertThat(failureCount.get()).isEqualTo(threadCount); // 모든 요청이 실패
    }
}
