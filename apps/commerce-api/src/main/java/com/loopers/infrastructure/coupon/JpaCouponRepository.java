package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCouponRepository extends JpaRepository<Coupon, Long>, CouponRepository {
}