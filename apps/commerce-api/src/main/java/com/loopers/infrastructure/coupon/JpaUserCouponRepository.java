package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaUserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.used = false")
    List<UserCoupon> findByUserIdAndUsedFalse(@Param("userId") String userId);
    
    /**
     * 동시성 제어를 위한 비관적 락 적용
     * 여러 사용자가 같은 쿠폰을 동시에 사용하려 할 때 정합성 보장
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithLock(@Param("id") Long id);
}