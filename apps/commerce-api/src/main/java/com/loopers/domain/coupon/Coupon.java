package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
public class Coupon extends BaseEntity {

    private String name;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private BigDecimal discountValue; // 정액: 원 단위, 정률: 퍼센트 (10% = 10.00)

    private BigDecimal maxDiscountAmount; // 정률 쿠폰의 최대 할인 금액 (null이면 제한 없음)

    private BigDecimal minOrderAmount; // 최소 주문 금액 조건 (null이면 제한 없음)

    protected Coupon() {
    }

    public Coupon(String name, CouponType couponType, BigDecimal discountValue, 
                  BigDecimal maxDiscountAmount, BigDecimal minOrderAmount) {
        validateName(name);
        validateCouponType(couponType);
        validateDiscountValue(discountValue);
        
        this.name = name;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
    }

    /**
     * 주문 금액에 대해 쿠폰 할인을 적용합니다.
     * @param orderAmount 주문 금액
     * @return 할인된 금액
     */
    public Money applyDiscount(Money orderAmount) {
        if (minOrderAmount != null && orderAmount.getValue() < minOrderAmount.longValue()) {
            throw new CoreException(ErrorType.INVALID_COUPON_CONDITION);
        }

        BigDecimal discount = calculateDiscount(BigDecimal.valueOf(orderAmount.getValue()));
        long discountedAmount = orderAmount.getValue() - discount.longValue();
        
        return new Money(Math.max(0, discountedAmount)); // 음수가 될 수 없음
    }

    /**
     * 할인 금액을 계산합니다.
     */
    public BigDecimal calculateDiscountAmount(Money orderAmount) {
        if (minOrderAmount != null && orderAmount.getValue() < minOrderAmount.longValue()) {
            return BigDecimal.ZERO;
        }

        return calculateDiscount(BigDecimal.valueOf(orderAmount.getValue()));
    }

    private BigDecimal calculateDiscount(BigDecimal orderAmount) {
        BigDecimal discount;
        
        if (couponType == CouponType.FIXED_AMOUNT) {
            discount = discountValue;
        } else { // PERCENTAGE
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));
            
            // 최대 할인 금액 제한 적용
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        }
        
        // 할인 금액이 주문 금액을 초과할 수 없음
        return discount.min(orderAmount);
    }

    public boolean canApply(Money orderAmount) {
        if (minOrderAmount == null) {
            return true;
        }
        return orderAmount.getValue() >= minOrderAmount.longValue();
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수입니다");
        }
    }

    private void validateCouponType(CouponType couponType) {
        if (couponType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다");
        }
    }

    private void validateDiscountValue(BigDecimal discountValue) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인값은 0보다 큰 값이어야 합니다");
        }
    }
}