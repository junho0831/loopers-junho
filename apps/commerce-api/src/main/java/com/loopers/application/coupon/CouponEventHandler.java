package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class CouponEventHandler {
    private static final Logger log = LoggerFactory.getLogger(CouponEventHandler.class);
    
    private final CouponService couponService;
    
    public CouponEventHandler(CouponService couponService) {
        this.couponService = couponService;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponUsage(OrderCreatedEvent event) {
        if (event.getCouponId() == null) {
            return;
        }
        
        log.info("쿠폰 사용 처리 시작 - couponId: {}, orderId: {}", event.getCouponId(), event.getOrderId());
        
        try {
            UserCoupon userCoupon = couponService.findUserCouponById(event.getCouponId());
            if (userCoupon != null && !userCoupon.isUsed()) {
                couponService.useCoupon(userCoupon, event.getOrderId());
                log.info("쿠폰 사용 완료 - couponId: {}, orderId: {}", event.getCouponId(), event.getOrderId());
            }
        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패 - couponId: {}, orderId: {}",
                     event.getCouponId(), event.getOrderId(), e);
        }
    }
}