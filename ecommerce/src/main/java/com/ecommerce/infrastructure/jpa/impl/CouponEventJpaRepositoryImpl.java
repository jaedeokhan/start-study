package com.ecommerce.infrastructure.jpa.impl;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.infrastructure.jpa.CouponEventJpaRepository;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponEventJpaRepositoryImpl implements CouponEventRepository {

    private final CouponEventJpaRepository couponEventJpaRepository;

    @Override
    public Optional<CouponEvent> findById(Long id) {
        return couponEventJpaRepository.findById(id);
    }

    @Override
    public List<CouponEvent> findAll() {
        return couponEventJpaRepository.findAll();
    }

    @Override
    public CouponEvent save(CouponEvent couponEvent) {
        return couponEventJpaRepository.save(couponEvent);
    }

    @Override
    public void issueCoupon(Long couponEventId) {
        CouponEvent couponEvent = couponEventJpaRepository.findByIdWithLock(couponEventId)
                .orElseThrow(() -> new
                        CouponEventNotFoundException(CouponErrorCode.COUPON_EVENT_NOT_FOUND));

        couponEvent.issue();
    }
}
