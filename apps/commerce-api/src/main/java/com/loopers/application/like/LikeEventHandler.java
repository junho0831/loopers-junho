package com.loopers.application.like;

import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.like.ProductLikeEvent;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Optional;

@Component
public class LikeEventHandler {
    private static final Logger log = LoggerFactory.getLogger(LikeEventHandler.class);
    
    private final JpaProductRepository productRepository;
    private final ProductCacheService cacheService;
    
    public LikeEventHandler(JpaProductRepository productRepository, ProductCacheService cacheService) {
        this.productRepository = productRepository;
        this.cacheService = cacheService;
    }
    
    @EventListener
    public void handleProductLikeEvent(ProductLikeEvent event) {
        System.out.println("DEBUG: Handling ProductLikeEvent - userId: " + event.getUserId() + ", productId: " + event.getProductId() + ", action: " + event.getAction());
        log.info("좋아요 이벤트 처리 시작 - userId: {}, productId: {}, action: {}", 
                event.getUserId(), event.getProductId(), event.getAction());
        
        try {
            // 1. 상품 좋아요 수 집계 업데이트
            updateProductLikeCount(event);
            
            // 2. 관련 캐시 무효화 (비동기)
            invalidateProductCaches(event.getProductId());
            
            log.info("좋아요 이벤트 처리 완료 - userId: {}, productId: {}, action: {}", 
                    event.getUserId(), event.getProductId(), event.getAction());
            
        } catch (Exception e) {
            log.error("좋아요 이벤트 처리 실패 - userId: {}, productId: {}, action: {}", 
                     event.getUserId(), event.getProductId(), event.getAction(), e);
        }
    }
    
    @Transactional
    public void updateProductLikeCount(ProductLikeEvent event) {
        try {
            Optional<Product> productOpt = productRepository.findByIdWithLock(event.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                if (event.isLiked()) {
                    product.incrementLikesCount();
                    log.debug("상품 좋아요 수 증가 - productId: {}", event.getProductId());
                } else {
                    product.decrementLikesCount();
                    log.debug("상품 좋아요 수 감소 - productId: {}", event.getProductId());
                }
                
                productRepository.save(product);
                log.info("상품 좋아요 수 업데이트 완료 - productId: {}, count: {}", 
                        event.getProductId(), product.getLikesCount());
            } else {
                log.warn("상품을 찾을 수 없음 - productId: {}", event.getProductId());
            }
        } catch (Exception e) {
            log.error("상품 좋아요 수 업데이트 실패 - productId: {}", event.getProductId(), e);
            // 집계 실패해도 예외를 던지지 않아 좋아요 처리에는 영향 없음
        }
    }
    
    @Async
    public void invalidateProductCaches(Long productId) {
        try {
            cacheService.evictProductCaches(productId);
            log.debug("상품 캐시 무효화 완료 - productId: {}", productId);
        } catch (Exception e) {
            log.error("상품 캐시 무효화 실패 - productId: {}", productId, e);
            // 캐시 실패해도 예외를 던지지 않아 좋아요 처리에는 영향 없음
        }
    }
}