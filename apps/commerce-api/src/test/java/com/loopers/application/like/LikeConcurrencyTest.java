package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class LikeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private JpaProductRepository productRepository;

    @Autowired
    private JpaBrandRepository brandRepository;

    @Autowired
    private JpaProductLikeRepository productLikeRepository;

    private Brand testBrand;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        productLikeRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();

        // 테스트용 브랜드 생성
        testBrand = new Brand("Like Test Brand");
        brandRepository.save(testBrand);

        // 테스트용 상품 생성
        testProduct = new Product("Popular Product", new Money(15000), new Stock(50), testBrand);
        productRepository.save(testProduct);
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러명이 좋아요/싫어요를 요청해도, 상품의 좋아요 개수가 정상 반영되어야 한다")
    void concurrency_multipleUsersLikeSameProduct_correctLikeCount() throws InterruptedException {
        // given
        int userCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when - 10명이 동시에 좋아요
        for (int i = 0; i < userCount; i++) {
            final String userId = "user-" + i;
            
            executor.submit(() -> {
                try {
                    likeFacade.addLike(userId, testProduct.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("좋아요 실패 (" + userId + "): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        long totalLikes = productLikeRepository.count();
        
        System.out.println("=== 좋아요 동시성 테스트 결과 ===");
        System.out.println("성공한 좋아요: " + successCount.get());
        System.out.println("실패한 좋아요: " + failureCount.get());
        System.out.println("상품 좋아요 수: " + updatedProduct.getLikesCount());
        System.out.println("DB 좋아요 레코드 수: " + totalLikes);

        // 모든 좋아요가 성공해야 함
        assertThat(successCount.get()).isEqualTo(userCount).as("모든 사용자의 좋아요가 성공해야 함");
        assertThat(failureCount.get()).isEqualTo(0).as("실패한 좋아요가 없어야 함");
        
        // 상품의 좋아요 수가 정확해야 함
        assertThat(updatedProduct.getLikesCount()).isEqualTo(userCount).as("상품 좋아요 수가 정확해야 함");
        assertThat(totalLikes).isEqualTo(userCount).as("DB에 저장된 좋아요 수가 정확해야 함");
    }

    @Test
    @DisplayName("동일한 상품에 대해 좋아요와 좋아요 취소를 동시에 요청할 때 정합성이 보장되어야 한다")
    void concurrency_likeAndUnlikeSameProduct_dataConsistency() throws InterruptedException {
        // given
        int userCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);
        
        AtomicInteger likeSuccessCount = new AtomicInteger(0);
        AtomicInteger unlikeSuccessCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 절반의 사용자는 미리 좋아요를 누른 상태로 만들기
        for (int i = 0; i < userCount / 2; i++) {
            String userId = "pre-liked-user-" + i;
            likeFacade.addLike(userId, testProduct.getId());
        }

        // when - 절반은 좋아요, 절반은 좋아요 취소를 동시 요청
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            
            executor.submit(() -> {
                try {
                    if (userIndex < userCount / 2) {
                        // 이미 좋아요를 누른 사용자들이 좋아요 취소
                        String userId = "pre-liked-user-" + userIndex;
                        likeFacade.removeLike(userId, testProduct.getId());
                        unlikeSuccessCount.incrementAndGet();
                    } else {
                        // 새로운 사용자들이 좋아요
                        String userId = "new-user-" + userIndex;
                        likeFacade.addLike(userId, testProduct.getId());
                        likeSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("좋아요/취소 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        long totalLikes = productLikeRepository.count();
        
        System.out.println("=== 좋아요/취소 동시성 테스트 결과 ===");
        System.out.println("좋아요 성공: " + likeSuccessCount.get());
        System.out.println("좋아요 취소 성공: " + unlikeSuccessCount.get());
        System.out.println("실패: " + failureCount.get());
        System.out.println("최종 상품 좋아요 수: " + updatedProduct.getLikesCount());
        System.out.println("DB 좋아요 레코드 수: " + totalLikes);

        // 예상 결과: 초기 4개 좋아요 - 4개 취소 + 4개 새로운 좋아요 = 4개
        int expectedLikes = userCount / 2; // 새로 추가된 좋아요 수
        
        assertThat(likeSuccessCount.get()).isEqualTo(userCount / 2).as("새로운 좋아요가 모두 성공해야 함");
        assertThat(unlikeSuccessCount.get()).isEqualTo(userCount / 2).as("좋아요 취소가 모두 성공해야 함");
        assertThat(failureCount.get()).isEqualTo(0).as("실패가 없어야 함");
        assertThat(updatedProduct.getLikesCount()).isEqualTo(expectedLikes).as("최종 좋아요 수가 정확해야 함");
        assertThat(totalLikes).isEqualTo(expectedLikes).as("DB 좋아요 수가 정확해야 함");
    }

    @Test
    @DisplayName("같은 사용자가 동시에 여러 번 좋아요를 누를 때 한 번만 적용되어야 한다 (멱등성)")
    void concurrency_sameUserMultipleLikes_idempotent() throws InterruptedException {
        // given
        int attemptCount = 5;
        String userId = "duplicate-user";
        ExecutorService executor = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);
        
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 같은 사용자가 동시에 5번 좋아요 시도
        for (int i = 0; i < attemptCount; i++) {
            executor.submit(() -> {
                try {
                    likeFacade.addLike(userId, testProduct.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 멱등성으로 인해 추가 시도는 무시되거나 예외가 발생할 수 있음
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        long totalLikes = productLikeRepository.count();
        
        System.out.println("=== 중복 좋아요 멱등성 테스트 결과 ===");
        System.out.println("좋아요 성공 횟수: " + successCount.get());
        System.out.println("상품 좋아요 수: " + updatedProduct.getLikesCount());
        System.out.println("DB 좋아요 레코드 수: " + totalLikes);

        // 멱등성에 의해 한 번만 적용되어야 함
        assertThat(updatedProduct.getLikesCount()).isEqualTo(1).as("좋아요는 한 번만 적용되어야 함");
        assertThat(totalLikes).isEqualTo(1).as("DB에는 하나의 좋아요만 저장되어야 함");
    }
}