package com.loopers.domain.coupon;

import com.loopers.domain.product.Money;
import com.loopers.infrastructure.coupon.JpaUserCouponRepository;
import org.springframework.stereotype.Service;

@Service
public class CouponService {
    private final JpaUserCouponRepository userCouponRepository;

    public CouponService(JpaUserCouponRepository userCouponRepository) {
        this.userCouponRepository = userCouponRepository;
    }

    /**
     * 쿠폰을 사용하여 주문 금액에 할인을 적용합니다.
     */
    public Money applyCouponDiscount(Coupon coupon, Money orderAmount) {
        if (!coupon.canApply(orderAmount)) {
            throw new IllegalArgumentException("Coupon cannot be applied to this order amount");
        }
        
        return coupon.applyDiscount(orderAmount);
    }

    /**
     * 쿠폰 할인 금액을 계산합니다.
     */
    public Money calculateDiscountAmount(Coupon coupon, Money orderAmount) {
        if (!coupon.canApply(orderAmount)) {
            return Money.ZERO;
        }
        
        long discountAmount = coupon.calculateDiscountAmount(orderAmount).longValue();
        return new Money(discountAmount);
    }

    /**
     * 사용자 쿠폰의 사용 가능 여부를 검증합니다.
     */
    public void validateUserCoupon(UserCoupon userCoupon, String userId, Money orderAmount) {
        if (!userCoupon.belongsTo(userId)) {
            throw new IllegalArgumentException("Coupon does not belong to the user");
        }
        
        if (!userCoupon.isUsable()) {
            throw new IllegalArgumentException("Coupon has already been used");
        }
        
        if (!userCoupon.getCoupon().canApply(orderAmount)) {
            throw new IllegalArgumentException("Order amount does not meet coupon requirements");
        }
    }

    /**
     * 사용자 쿠폰을 로드하고 검증합니다.
     */
    public UserCoupon loadAndValidateUserCoupon(Long couponId, String userId, Money orderAmount) {
        UserCoupon userCoupon = userCouponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId));
        
        validateUserCoupon(userCoupon, userId, orderAmount);
        return userCoupon;
    }

    /**
     * 사용자 쿠폰을 사용 처리합니다.
     */
    public void useCoupon(UserCoupon userCoupon, Long orderId) {
        userCoupon.use(orderId);
        userCouponRepository.save(userCoupon);
    }

    /**
     * 쿠폰 사용을 취소합니다. (주문 실패 시)
     */
    public void cancelCouponUsage(UserCoupon userCoupon) {
        userCoupon.cancelUsage();
        userCouponRepository.save(userCoupon);
    }
}