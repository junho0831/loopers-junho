package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderRequest;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.coupon.JpaCouponRepository;
import com.loopers.infrastructure.coupon.JpaUserCouponRepository;
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
public class CouponConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private JpaPointRepository pointRepository;

    @Autowired
    private JpaBrandRepository brandRepository;

    @Autowired
    private JpaCouponRepository couponRepository;

    @Autowired
    private JpaUserCouponRepository userCouponRepository;

    @Autowired
    private JpaOrderRepository orderRepository;

    private Brand testBrand;
    private Product testProduct;
    private Coupon fixedAmountCoupon;
    private Coupon percentageCoupon;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        orderRepository.deleteAll();
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        pointRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();

        // 테스트용 브랜드 생성
        testBrand = new Brand("Coupon Test Brand");
        brandRepository.save(testBrand);

        // 테스트용 상품 생성 (재고 충분)
        testProduct = new Product("Test Product", new Money(10000), new Stock(100), testBrand);
        productRepository.save(testProduct);

        // 정액 쿠폰 생성 (5000원 할인)
        fixedAmountCoupon = new Coupon("5000원 할인쿠폰", CouponType.FIXED_AMOUNT, 
                BigDecimal.valueOf(5000), null, null);
        couponRepository.save(fixedAmountCoupon);

        // 정률 쿠폰 생성 (10% 할인, 최대 3000원)
        percentageCoupon = new Coupon("10% 할인쿠폰", CouponType.PERCENTAGE, 
                BigDecimal.valueOf(10), BigDecimal.valueOf(3000), null);
        couponRepository.save(percentageCoupon);
    }

    @Test
    @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한번만 사용되어야 한다")
    void concurrency_sameCouponMultipleUsers_onlyOneSuccess() throws InterruptedException {
        // given
        int competitorCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(competitorCount);
        CountDownLatch latch = new CountDownLatch(competitorCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 여러 사용자에게 같은 쿠폰 지급 (실제로는 불가능하지만 테스트를 위해)
        UserCoupon sharedCoupon = new UserCoupon("user-0", fixedAmountCoupon);
        userCouponRepository.save(sharedCoupon);

        // 각 사용자에게 충분한 포인트 제공
        for (int i = 0; i < competitorCount; i++) {
            String userId = "user-" + i;
            Point userPoint = new Point(userId, BigDecimal.valueOf(20000));
            pointRepository.save(userPoint);
        }

        // when - 5명이 동시에 같은 쿠폰으로 주문
        for (int i = 0; i < competitorCount; i++) {
            final String userId = "user-" + i;
            
            executor.submit(() -> {
                try {
                    OrderRequest orderRequest = new OrderRequest(
                            List.of(new OrderItemRequest(testProduct.getId(), 1)),
                            sharedCoupon.getId()
                    );
                    
                    orderFacade.createOrderWithCoupon(userId, orderRequest);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("쿠폰 사용 실패 (" + userId + "): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        UserCoupon updatedCoupon = userCouponRepository.findById(sharedCoupon.getId()).orElseThrow();
        long totalOrders = orderRepository.count();
        
        System.out.println("=== 쿠폰 동시성 테스트 결과 ===");
        System.out.println("성공한 주문: " + successCount.get());
        System.out.println("실패한 주문: " + failureCount.get());
        System.out.println("쿠폰 사용 여부: " + updatedCoupon.isUsed());
        System.out.println("총 주문 수: " + totalOrders);

        // 정확히 한 명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1).as("쿠폰은 정확히 한 번만 사용되어야 함");
        assertThat(failureCount.get()).isEqualTo(competitorCount - 1).as("나머지 사용자들은 실패해야 함");
        assertThat(updatedCoupon.isUsed()).isTrue().as("쿠폰이 사용됨으로 표시되어야 함");
        assertThat(totalOrders).isEqualTo(1).as("성공한 주문만 저장되어야 함");
    }

    @Test
    @DisplayName("정액 쿠폰과 정률 쿠폰의 할인 적용이 정확해야 한다")
    void concurrency_differentCouponTypes_correctDiscountApplication() throws InterruptedException {
        // given
        int userCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);
        
        AtomicInteger successCount = new AtomicInteger(0);

        // 각 사용자에게 개별 쿠폰 지급
        UserCoupon fixedCoupon1 = new UserCoupon("fixed-user-1", fixedAmountCoupon);
        UserCoupon fixedCoupon2 = new UserCoupon("fixed-user-2", fixedAmountCoupon);
        UserCoupon percentCoupon1 = new UserCoupon("percent-user-1", percentageCoupon);
        UserCoupon percentCoupon2 = new UserCoupon("percent-user-2", percentageCoupon);
        
        userCouponRepository.save(fixedCoupon1);
        userCouponRepository.save(fixedCoupon2);
        userCouponRepository.save(percentCoupon1);
        userCouponRepository.save(percentCoupon2);

        // 각 사용자에게 충분한 포인트 제공
        Point fixedUser1Point = new Point("fixed-user-1", BigDecimal.valueOf(20000));
        Point fixedUser2Point = new Point("fixed-user-2", BigDecimal.valueOf(20000));
        Point percentUser1Point = new Point("percent-user-1", BigDecimal.valueOf(20000));
        Point percentUser2Point = new Point("percent-user-2", BigDecimal.valueOf(20000));
        
        pointRepository.save(fixedUser1Point);
        pointRepository.save(fixedUser2Point);
        pointRepository.save(percentUser1Point);
        pointRepository.save(percentUser2Point);

        // when - 동시에 다른 종류의 쿠폰으로 주문
        executor.submit(() -> {
            try {
                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderItemRequest(testProduct.getId(), 1)), // 10000원
                        fixedCoupon1.getId()
                );
                orderFacade.createOrderWithCoupon("fixed-user-1", orderRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("정액 쿠폰 사용 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderItemRequest(testProduct.getId(), 2)), // 20000원
                        fixedCoupon2.getId()
                );
                orderFacade.createOrderWithCoupon("fixed-user-2", orderRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("정액 쿠폰 사용 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderItemRequest(testProduct.getId(), 1)), // 10000원
                        percentCoupon1.getId()
                );
                orderFacade.createOrderWithCoupon("percent-user-1", orderRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("정률 쿠폰 사용 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderItemRequest(testProduct.getId(), 3)), // 30000원
                        percentCoupon2.getId()
                );
                orderFacade.createOrderWithCoupon("percent-user-2", orderRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("정률 쿠폰 사용 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        // then
        long totalOrders = orderRepository.count();
        
        System.out.println("=== 쿠폰 할인 적용 테스트 결과 ===");
        System.out.println("성공한 주문: " + successCount.get());
        System.out.println("총 주문 수: " + totalOrders);

        // 각 사용자별 포인트 확인으로 할인 적용 검증
        Point updatedFixed1Point = pointRepository.findByUserId("fixed-user-1").orElseThrow();
        Point updatedFixed2Point = pointRepository.findByUserId("fixed-user-2").orElseThrow();
        Point updatedPercent1Point = pointRepository.findByUserId("percent-user-1").orElseThrow();
        Point updatedPercent2Point = pointRepository.findByUserId("percent-user-2").orElseThrow();

        // 정액 쿠폰: 10000 - 5000 = 5000원 차감
        assertThat(updatedFixed1Point.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        // 정액 쿠폰: 20000 - 5000 = 15000원 차감
        assertThat(updatedFixed2Point.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        // 정률 쿠폰: 10000 - (10000 * 0.1) = 9000원 차감
        assertThat(updatedPercent1Point.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(11000));
        // 정률 쿠폰: 30000 - 3000(최대할인) = 27000원 차감 → 포인트 잔액: 20000 - 27000 = -7000
        // 하지만 실제로는 포인트 부족으로 주문이 실패했으므로 원래 잔액 유지
        assertThat(updatedPercent2Point.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(20000));

        assertThat(successCount.get()).isEqualTo(3).as("포인트 부족한 1건을 제외하고 3건 성공해야 함");
        assertThat(totalOrders).isEqualTo(3).as("성공한 3건의 주문만 저장되어야 함");
    }
}