package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.order.JpaOrderRepository;
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
public class OrderE2EConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private JpaPointRepository pointRepository;

    @Autowired
    private JpaBrandRepository brandRepository;

    @Autowired
    private JpaOrderRepository orderRepository;

    private Brand brand;
    private Product limitedProduct;
    private Product unlimitedProduct;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        orderRepository.deleteAll();
        pointRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();

        // 테스트용 브랜드 생성
        brand = new Brand("Limited Edition Brand");
        brandRepository.save(brand);

        // 한정판 상품 (재고 5개)
        limitedProduct = new Product("Limited Edition Sneakers", new Money(50000), new Stock(5), brand);
        productRepository.save(limitedProduct);

        // 일반 상품 (재고 충분)
        unlimitedProduct = new Product("Regular T-Shirt", new Money(20000), new Stock(100), brand);
        productRepository.save(unlimitedProduct);
    }

    @Test
    @DisplayName("한정판 상품 동시 주문 시나리오 - 정합성 보장")
    void e2e_limitedProductConcurrentOrders_shouldMaintainDataIntegrity() throws InterruptedException {
        // given
        int totalCustomers = 10; // 10명의 고객이 동시에 구매 시도
        int limitedStock = 5; // 한정판 상품 재고 5개
        
        ExecutorService executor = Executors.newFixedThreadPool(totalCustomers);
        CountDownLatch latch = new CountDownLatch(totalCustomers);
        
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        // 각 고객마다 충분한 포인트 제공
        for (int i = 0; i < totalCustomers; i++) {
            String customerId = "customer-" + i;
            Point customerPoint = new Point(customerId, BigDecimal.valueOf(100000));
            pointRepository.save(customerPoint);
        }

        // when - 10명이 동시에 한정판 상품 1개씩 주문
        for (int i = 0; i < totalCustomers; i++) {
            final String customerId = "customer-" + i;
            
            executor.submit(() -> {
                try {
                    List<OrderItemRequest> itemRequests = List.of(
                            new OrderItemRequest(limitedProduct.getId(), 1)
                    );
                    
                    orderFacade.createOrder(customerId, itemRequests);
                    successfulOrders.incrementAndGet();
                    
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                    System.out.println("고객 " + customerId + " 주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then - 결과 검증
        Product updatedLimitedProduct = productRepository.findById(limitedProduct.getId()).orElseThrow();
        long totalSuccessfulOrders = orderRepository.count();
        
        System.out.println("=== 한정판 상품 동시 주문 결과 ===");
        System.out.println("성공한 주문 수: " + successfulOrders.get());
        System.out.println("실패한 주문 수: " + failedOrders.get());
        System.out.println("남은 재고: " + updatedLimitedProduct.getStock().getQuantity());
        System.out.println("DB에 저장된 주문 수: " + totalSuccessfulOrders);

        // 핵심 검증사항들
        assertThat(successfulOrders.get()).isEqualTo(limitedStock) // 정확히 5개 주문만 성공
                .as("한정판 상품은 재고 수량만큼만 주문이 성공해야 함");
                
        assertThat(failedOrders.get()).isEqualTo(totalCustomers - limitedStock) // 나머지 5개 주문은 실패
                .as("재고를 초과한 주문은 모두 실패해야 함");
                
        assertThat(updatedLimitedProduct.getStock().getQuantity()).isEqualTo(0) // 재고는 정확히 0
                .as("모든 재고가 정확히 차감되어야 함");
                
        assertThat(totalSuccessfulOrders).isEqualTo(successfulOrders.get()) // DB 저장된 주문 수 일치
                .as("성공한 주문은 모두 DB에 저장되어야 함");
    }

    @Test
    @DisplayName("복합 상품 주문 동시성 시나리오 - 한정판 + 일반 상품")
    void e2e_mixedProductConcurrentOrders_shouldHandleComplexScenario() throws InterruptedException {
        // given
        int totalCustomers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(totalCustomers);
        CountDownLatch latch = new CountDownLatch(totalCustomers);
        
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        // 각 고객마다 충분한 포인트 제공
        for (int i = 0; i < totalCustomers; i++) {
            String customerId = "mixed-customer-" + i;
            Point customerPoint = new Point(customerId, BigDecimal.valueOf(200000));
            pointRepository.save(customerPoint);
        }

        // when - 8명이 동시에 한정판 1개 + 일반상품 2개씩 주문
        for (int i = 0; i < totalCustomers; i++) {
            final String customerId = "mixed-customer-" + i;
            
            executor.submit(() -> {
                try {
                    List<OrderItemRequest> itemRequests = List.of(
                            new OrderItemRequest(limitedProduct.getId(), 1),   // 한정판 1개 (50,000원)
                            new OrderItemRequest(unlimitedProduct.getId(), 2)  // 일반상품 2개 (40,000원)
                    );
                    
                    orderFacade.createOrder(customerId, itemRequests);
                    successfulOrders.incrementAndGet();
                    
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                    System.out.println("고객 " + customerId + " 복합 주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        Product updatedLimitedProduct = productRepository.findById(limitedProduct.getId()).orElseThrow();
        Product updatedUnlimitedProduct = productRepository.findById(unlimitedProduct.getId()).orElseThrow();
        
        System.out.println("=== 복합 상품 동시 주문 결과 ===");
        System.out.println("성공한 주문 수: " + successfulOrders.get());
        System.out.println("실패한 주문 수: " + failedOrders.get());
        System.out.println("한정판 남은 재고: " + updatedLimitedProduct.getStock().getQuantity());
        System.out.println("일반상품 남은 재고: " + updatedUnlimitedProduct.getStock().getQuantity());

        // 한정판은 최대 5개까지만 주문 가능
        int expectedLimitedRemaining = Math.max(0, 5 - successfulOrders.get());
        assertThat(updatedLimitedProduct.getStock().getQuantity()).isEqualTo(expectedLimitedRemaining);
        
        // 일반상품은 성공한 주문 수 * 2만큼 차감
        int expectedUnlimitedRemaining = 100 - (successfulOrders.get() * 2);
        assertThat(updatedUnlimitedProduct.getStock().getQuantity()).isEqualTo(expectedUnlimitedRemaining);
        
        // 성공 + 실패 = 전체 시도 수
        assertThat(successfulOrders.get() + failedOrders.get()).isEqualTo(totalCustomers);
    }

    @Test
    @DisplayName("포인트 부족 시나리오 - 동시성 상황에서 정확한 포인트 검증")
    void e2e_insufficientPoints_shouldRejectOrdersCorrectly() throws InterruptedException {
        // given
        int totalCustomers = 6;
        BigDecimal limitedPoints = BigDecimal.valueOf(30000); // 한정판(50,000원)보다 적은 포인트
        
        ExecutorService executor = Executors.newFixedThreadPool(totalCustomers);
        CountDownLatch latch = new CountDownLatch(totalCustomers);
        
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        // 각 고객에게 부족한 포인트만 제공
        for (int i = 0; i < totalCustomers; i++) {
            String customerId = "poor-customer-" + i;
            Point customerPoint = new Point(customerId, limitedPoints);
            pointRepository.save(customerPoint);
        }

        // when - 6명이 동시에 한정판 상품 주문 (포인트 부족)
        for (int i = 0; i < totalCustomers; i++) {
            final String customerId = "poor-customer-" + i;
            
            executor.submit(() -> {
                try {
                    List<OrderItemRequest> itemRequests = List.of(
                            new OrderItemRequest(limitedProduct.getId(), 1) // 50,000원 상품
                    );
                    
                    orderFacade.createOrder(customerId, itemRequests);
                    successfulOrders.incrementAndGet();
                    
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then - 모든 주문이 실패해야 함
        Product updatedProduct = productRepository.findById(limitedProduct.getId()).orElseThrow();
        long totalOrders = orderRepository.count();
        
        System.out.println("=== 포인트 부족 시나리오 결과 ===");
        System.out.println("성공한 주문 수: " + successfulOrders.get());
        System.out.println("실패한 주문 수: " + failedOrders.get());
        System.out.println("상품 남은 재고: " + updatedProduct.getStock().getQuantity());
        
        assertThat(successfulOrders.get()).isEqualTo(0).as("포인트가 부족하면 모든 주문이 실패해야 함");
        assertThat(failedOrders.get()).isEqualTo(totalCustomers).as("모든 고객의 주문이 실패해야 함");
        assertThat(updatedProduct.getStock().getQuantity()).isEqualTo(5).as("재고는 변경되지 않아야 함");
        assertThat(totalOrders).isEqualTo(0).as("실패한 주문은 DB에 저장되지 않아야 함");
    }
}
