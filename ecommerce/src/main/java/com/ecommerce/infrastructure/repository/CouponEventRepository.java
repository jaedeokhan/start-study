package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.coupon.CouponEvent;

import java.util.List;
import java.util.Optional;

/**
 * CouponEvent Repository Interface
 */
public interface CouponEventRepository {
    Optional<CouponEvent> findById(Long id);
    List<CouponEvent> findAll();
    CouponEvent save(CouponEvent couponEvent);
    void issueCoupon(Long couponEventId);
}
