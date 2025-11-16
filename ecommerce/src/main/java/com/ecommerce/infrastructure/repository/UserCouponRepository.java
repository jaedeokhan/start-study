package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long>  {
    Optional<UserCoupon> findById(Long id);

    List<UserCoupon> findByUserId(Long userId);

    boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId);

    UserCoupon save(UserCoupon userCoupon);

    long countByUserId(Long userId);
}
