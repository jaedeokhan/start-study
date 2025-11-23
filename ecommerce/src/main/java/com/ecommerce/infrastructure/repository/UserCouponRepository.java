package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long>  {

    default UserCoupon findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new CouponNotFoundException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    List<UserCoupon> findByUserId(Long userId);

    boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId);

    UserCoupon save(UserCoupon userCoupon);

    long countByUserId(Long userId);
}
