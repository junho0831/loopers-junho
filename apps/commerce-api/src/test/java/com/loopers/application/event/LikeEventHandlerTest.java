package com.loopers.application.event;

import com.loopers.application.like.LikeEventHandler;
import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.like.ProductLikeEvent;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LikeEventHandler 단위 테스트
 * 좋아요 이벤트 처리 및 집계 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class LikeEventHandlerTest {

    @Mock
    private JpaProductRepository productRepository;
    
    @Mock
    private ProductCacheService cacheService;

    @InjectMocks
    private LikeEventHandler likeEventHandler;

    @Test
    @DisplayName("좋아요 이벤트 처리 시 상품 집계가 증가하는지 확인")
    void testHandleProductLikeEventIncrement() {
        // Given
        Long productId = 1L;
        ProductLikeEvent likeEvent = ProductLikeEvent.liked("test-user", productId);
        Product mockProduct = mock(Product.class);
        
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(mockProduct));

        // When
        likeEventHandler.handleProductLikeEvent(likeEvent);

        // Then
        verify(mockProduct).incrementLikesCount();
        verify(productRepository).save(mockProduct);
        verify(cacheService).evictProductCaches(productId);
    }

    @Test
    @DisplayName("좋아요 취소 이벤트 처리 시 상품 집계가 감소하는지 확인")
    void testHandleProductUnlikeEventDecrement() {
        // Given
        Long productId = 1L;
        ProductLikeEvent unlikeEvent = ProductLikeEvent.unliked("test-user", productId);
        Product mockProduct = mock(Product.class);
        
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(mockProduct));

        // When
        likeEventHandler.handleProductLikeEvent(unlikeEvent);

        // Then
        verify(mockProduct).decrementLikesCount();
        verify(productRepository).save(mockProduct);
        verify(cacheService).evictProductCaches(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품에 대한 이벤트 처리 시 예외가 발생하지 않는지 확인")
    void testHandleEventForNonExistentProduct() {
        // Given
        Long nonExistentProductId = 999L;
        ProductLikeEvent likeEvent = ProductLikeEvent.liked("test-user", nonExistentProductId);
        
        when(productRepository.findByIdWithLock(nonExistentProductId)).thenReturn(Optional.empty());

        // When & Then - 예외가 발생하지 않아야 함
        likeEventHandler.handleProductLikeEvent(likeEvent);
        
        // 상품이 없으므로 업데이트는 호출되지 않음
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("집계 업데이트 실패 시 예외가 전파되지 않는지 확인 (부가 로직 실패가 메인 로직에 영향 없음)")
    void testAggregationFailureDoesNotPropagate() {
        // Given
        Long productId = 1L;
        ProductLikeEvent likeEvent = ProductLikeEvent.liked("test-user", productId);
        Product mockProduct = mock(Product.class);
        
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(mockProduct));
        doThrow(new RuntimeException("DB 오류")).when(productRepository).save(any());

        // When & Then - 예외가 전파되지 않아야 함 (catch되어서 로깅만 됨)
        likeEventHandler.handleProductLikeEvent(likeEvent);
        
        verify(mockProduct).incrementLikesCount();
        verify(productRepository).save(mockProduct);
    }

    @Test
    @DisplayName("캐시 무효화 실패 시 예외가 전파되지 않는지 확인")
    void testCacheInvalidationFailureDoesNotPropagate() {
        // Given
        Long productId = 1L;
        
        doThrow(new RuntimeException("캐시 서버 오류")).when(cacheService).evictProductCaches(productId);

        // When & Then - 예외가 전파되지 않아야 함
        likeEventHandler.invalidateProductCaches(productId);
        
        verify(cacheService).evictProductCaches(productId);
    }

    @Test
    @DisplayName("집계 업데이트만 호출하는 메서드 단위 테스트")
    void testUpdateProductLikeCountOnly() {
        // Given
        ProductLikeEvent likeEvent = ProductLikeEvent.liked("test-user", 1L);
        Product mockProduct = mock(Product.class);
        
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockProduct));

        // When
        likeEventHandler.updateProductLikeCount(likeEvent);

        // Then
        verify(productRepository).findByIdWithLock(1L);
        verify(mockProduct).incrementLikesCount();
        verify(productRepository).save(mockProduct);
        
        // 캐시 무효화는 별도 메서드이므로 호출되지 않음
        verify(cacheService, never()).evictProductCaches(any());
    }

    @Test
    @DisplayName("동일한 상품에 대한 여러 이벤트 처리 시 락 충돌 없이 처리되는지 확인")
    void testConcurrentEventHandlingWithLock() {
        // Given
        Long productId = 1L;
        ProductLikeEvent event1 = ProductLikeEvent.liked("user1", productId);
        ProductLikeEvent event2 = ProductLikeEvent.liked("user2", productId);
        Product mockProduct = mock(Product.class);
        
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(mockProduct));

        // When
        likeEventHandler.updateProductLikeCount(event1);
        likeEventHandler.updateProductLikeCount(event2);

        // Then - 두 번 모두 정상 처리됨
        verify(productRepository, times(2)).findByIdWithLock(productId);
        verify(mockProduct, times(2)).incrementLikesCount();
        verify(productRepository, times(2)).save(mockProduct);
    }
}