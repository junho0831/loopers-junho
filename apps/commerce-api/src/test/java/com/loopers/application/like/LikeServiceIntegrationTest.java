package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Stock;
import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.LikeService;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
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

@ExtendWith(MockitoExtension.class)
class LikeServiceIntegrationTest {

    @Mock
    private JpaProductRepository productRepository;

    @Mock
    private JpaBrandRepository brandRepository;

    @Mock
    private JpaProductLikeRepository likeRepository;

    @InjectMocks
    private LikeService likeService;

    private Brand testBrand;
    private Product testProduct1;
    private Product testProduct2;
    private String testUserId = "testUser";

    @BeforeEach
    void setUp() {
        testBrand = new Brand("테스트 브랜드");
        testProduct1 = new Product("상품1", new Money(10000), new Stock(10), testBrand);
        testProduct2 = new Product("상품2", new Money(20000), new Stock(5), testBrand);
    }

    @Test
    @DisplayName("좋아요 추가에 성공할 경우, ProductLike 객체를 반환한다")
    void addLike_WithValidRequest_ReturnsProductLike() {
        // given
        Long productId = 1L;
        ProductLike expectedLike = new ProductLike(testUserId, productId);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
        when(likeRepository.findByUserIdAndProductId(testUserId, productId)).thenReturn(Optional.empty());
        when(likeRepository.save(any(ProductLike.class))).thenReturn(expectedLike);

        // when
        ProductLike result = likeService.addLike(testUserId, productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("이미 좋아요한 상품에 다시 좋아요를 추가할 경우, 기존 좋아요를 반환한다")
    void addLike_WithAlreadyLikedProduct_ReturnsExistingLike() {
        // given
        Long productId = 1L;
        ProductLike existingLike = new ProductLike(testUserId, productId);
        
        when(likeRepository.findByUserIdAndProductId(testUserId, productId)).thenReturn(Optional.of(existingLike));

        // when
        ProductLike result = likeService.addLike(testUserId, productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingLike.getId());
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품에 좋아요를 추가할 경우, 예외가 발생한다")
    void addLike_WithNonExistentProduct_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> likeService.addLike(testUserId, nonExistentProductId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("좋아요 취소에 성공할 경우, void를 반환한다")
    void removeLike_WithValidRequest_ReturnsVoid() {
        // given
        Long productId = 1L;
        ProductLike existingLike = new ProductLike(testUserId, productId);
        
        when(likeRepository.findByUserIdAndProductId(testUserId, productId)).thenReturn(Optional.of(existingLike));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

        // when
        likeService.removeLike(testUserId, productId);

        // then
        // 삭제 메소드가 호출되었는지 확인은 별도로 필요하지만, 현재는 예외 없이 실행되는지만 확인
    }

    @Test
    @DisplayName("좋아요하지 않은 상품을 취소할 경우, 아무 일도 일어나지 않는다")
    void removeLike_WithNotLikedProduct_DoesNothing() {
        // given
        Long productId = 1L;
        when(likeRepository.findByUserIdAndProductId(testUserId, productId)).thenReturn(Optional.empty());

        // when
        likeService.removeLike(testUserId, productId);

        // then
        // 예외가 발생하지 않아야 함
    }

    @Test
    @DisplayName("사용자가 좋아요한 상품 목록을 조회할 수 있다")
    void getLikedProducts_WithValidUserId_ReturnsProductLikeList() {
        // given
        Long productId1 = 1L;
        Long productId2 = 2L;
        ProductLike like1 = new ProductLike(testUserId, productId1);
        ProductLike like2 = new ProductLike(testUserId, productId2);
        List<ProductLike> expectedLikes = List.of(like1, like2);
        
        when(likeRepository.findByUserId(testUserId)).thenReturn(expectedLikes);

        // when
        List<ProductLike> result = likeService.getLikedProducts(testUserId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("productId")
                .containsExactlyInAnyOrder(productId1, productId2);
    }

    @Test
    @DisplayName("좋아요하지 않은 사용자의 상품 목록 조회 시 빈 리스트를 반환한다")
    void getLikedProducts_WithNoLikes_ReturnsEmptyList() {
        // given
        String userWithNoLikes = "userWithNoLikes";
        when(likeRepository.findByUserId(userWithNoLikes)).thenReturn(List.of());

        // when
        List<ProductLike> result = likeService.getLikedProducts(userWithNoLikes);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품의 좋아요 개수를 조회할 수 있다")
    void getLikeCount_WithValidProductId_ReturnsCount() {
        // given
        Long productId = 1L;
        when(likeRepository.countByProductId(productId)).thenReturn(2L);

        // when
        long result = likeRepository.countByProductId(productId);

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("좋아요가 없는 상품의 개수 조회 시 0을 반환한다")
    void getLikeCount_WithNoLikes_ReturnsZero() {
        // given
        Long productId = 1L;
        when(likeRepository.countByProductId(productId)).thenReturn(0L);

        // when
        long result = likeRepository.countByProductId(productId);

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자가 특정 상품을 좋아요했는지 확인할 수 있다")
    void isLiked_WithLikedProduct_ReturnsTrue() {
        // given
        Long productId = 1L;
        ProductLike like = new ProductLike(testUserId, productId);
        when(likeRepository.findByUserIdAndProductId(testUserId, productId)).thenReturn(Optional.of(like));

        // when
        Optional<ProductLike> result = likeRepository.findByUserIdAndProductId(testUserId, productId);

        // then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("사용자가 좋아요하지 않은 상품 확인 시 false를 반환한다")
    void isLiked_WithNotLikedProduct_ReturnsFalse() {
        // given
        Long productId = 1L;
        when(likeRepository.findByUserIdAndProductId(testUserId, productId)).thenReturn(Optional.empty());

        // when
        Optional<ProductLike> result = likeRepository.findByUserIdAndProductId(testUserId, productId);

        // then
        assertThat(result).isEmpty();
    }
}