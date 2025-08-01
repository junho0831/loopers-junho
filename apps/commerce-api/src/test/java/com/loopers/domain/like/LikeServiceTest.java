package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.brand.Brand;
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
class LikeServiceTest {

    @Mock
    private com.loopers.infrastructure.like.JpaProductLikeRepository productLikeRepository;

    @Mock
    private com.loopers.infrastructure.product.JpaProductRepository productRepository;

    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeService = new LikeService(productLikeRepository, productRepository);
    }

    @Test
    @DisplayName("좋아요를 추가할 수 있다")
    void addLike() {
        // given
        String userId = "user1";
        Long productId = 1L;
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new com.loopers.domain.product.Money(10000),
                new com.loopers.domain.product.Stock(10), brand);

        when(productLikeRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.empty());
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(productLikeRepository.save(any(ProductLike.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        // when
        ProductLike result = likeService.addLike(userId, productId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(product.getLikesCount()).isEqualTo(1);

        verify(productLikeRepository).save(any(ProductLike.class));
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("이미 좋아요가 있으면 중복 추가하지 않는다 (멱등성)")
    void addLikeWhenAlreadyLiked() {
        // given
        String userId = "user1";
        Long productId = 1L;
        ProductLike existingLike = new ProductLike(userId, productId);

        when(productLikeRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.of(existingLike));

        // when
        ProductLike result = likeService.addLike(userId, productId);

        // then
        assertThat(result).isEqualTo(existingLike);
        verify(productLikeRepository, never()).save(any(ProductLike.class));
        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품에 좋아요를 추가하면 예외가 발생한다")
    void addLikeWithNonExistentProduct() {
        // given
        String userId = "user1";
        Long productId = 999L;

        when(productLikeRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.empty());
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> likeService.addLike(userId, productId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("좋아요를 취소할 수 있다")
    void removeLike() {
        // given
        String userId = "user1";
        Long productId = 1L;
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new com.loopers.domain.product.Money(10000),
                new com.loopers.domain.product.Stock(10), brand);
        product.incrementLikesCount(); // 좋아요 수를 1로 설정

        ProductLike existingLike = new ProductLike(userId, productId);

        when(productLikeRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.of(existingLike));
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        // when
        likeService.removeLike(userId, productId);

        // then
        assertThat(product.getLikesCount()).isEqualTo(0);
        verify(productLikeRepository).delete(existingLike);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("좋아요가 없으면 취소해도 예외가 발생하지 않는다 (멱등성)")
    void removeLikeWhenNotLiked() {
        // given
        String userId = "user1";
        Long productId = 1L;

        when(productLikeRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatCode(() -> likeService.removeLike(userId, productId))
                .doesNotThrowAnyException();

        verify(productLikeRepository, never()).delete(any());
        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("사용자의 좋아요 목록을 조회할 수 있다")
    void getLikedProducts() {
        // given
        String userId = "user1";
        List<ProductLike> expectedLikes = List.of(
                new ProductLike(userId, 1L),
                new ProductLike(userId, 2L));

        when(productLikeRepository.findByUserId(userId))
                .thenReturn(expectedLikes);

        // when
        List<ProductLike> result = likeService.getLikedProducts(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedLikes);
    }
}