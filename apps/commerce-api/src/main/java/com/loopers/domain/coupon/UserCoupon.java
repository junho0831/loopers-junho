package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_coupon")
public class UserCoupon extends BaseEntity {

    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    private boolean used;

    private LocalDateTime usedAt;

    private Long orderId; // 어떤 주문에서 사용되었는지

    protected UserCoupon() {
    }

    public UserCoupon(String userId, Coupon coupon) {
        validateUserId(userId);
        validateCoupon(coupon);
        
        this.userId = userId;
        this.coupon = coupon;
        this.used = false;
        this.usedAt = null;
        this.orderId = null;
    }

    /**
     * 쿠폰을 사용 처리합니다.
     * 이미 사용된 쿠폰은 다시 사용할 수 없습니다.
     */
    public void use(Long orderId) {
        if (this.used) {
            throw new CoreException(ErrorType.COUPON_ALREADY_USED);
        }
        
        this.used = true;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
    }

    /**
     * 쿠폰이 사용 가능한지 확인합니다.
     */
    public boolean isUsable() {
        return !used;
    }

    /**
     * 특정 사용자의 쿠폰인지 확인합니다.
     */
    public boolean belongsTo(String userId) {
        return this.userId.equals(userId);
    }

    /**
     * 쿠폰 사용을 취소합니다. (주문 실패 시 사용)
     */
    public void cancelUsage() {
        if (!this.used) {
            return; // 이미 미사용 상태
        }
        
        this.used = false;
        this.usedAt = null;
        this.orderId = null;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다");
        }
    }

    private void validateCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 정보는 필수입니다");
        }
    }
}