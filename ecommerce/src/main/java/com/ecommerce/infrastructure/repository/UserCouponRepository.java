package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.coupon.UserCoupon;

import java.util.List;
import java.util.Optional;

/**
 * UserCoupon Repository Interface
 */
public interface UserCouponRepository {
    Optional<UserCoupon> findById(Long id);
    List<UserCoupon> findByUserId(Long userId);
    boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId);
    UserCoupon save(UserCoupon userCoupon);
}
