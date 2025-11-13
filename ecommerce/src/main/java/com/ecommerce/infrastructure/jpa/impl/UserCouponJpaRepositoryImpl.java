package com.ecommerce.infrastructure.jpa.impl;

import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.infrastructure.jpa.UserCouponJpaRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCouponJpaRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository userCouponJpaRepository;

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return userCouponJpaRepository.findById(id);
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponJpaRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId) {
        return userCouponJpaRepository.existsByUserIdAndCouponEventId(userId, couponEventId);
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }
}