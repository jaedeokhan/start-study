package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import com.ecommerce.infrastructure.redis.CouponRedisRepository;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-COUP-002: 쿠폰 발급
 */
@Component
@RequiredArgsConstructor
public class AsyncIssueCouponUseCase {
    private final CouponEventRepository couponEventRepository;
    private final CouponRedisRepository couponRedisRepository;

    public IssueCouponResponse execute(Long couponEventId, Long userId) {
        CouponEvent couponEvent = couponEventRepository.findByIdOrThrow(couponEventId);

        if (!couponEvent.isActive()) {
            throw new CouponExpiredException(CouponErrorCode.COUPON_EXPIRED);
        }

        boolean issued = couponRedisRepository.tryIssueCoupon(couponEventId, userId);

        if (!issued) {
            if (couponRedisRepository.isAlreadyIssued(couponEventId, userId)) {
                throw new CouponAlreadyIssuedException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            } else {
                throw new CouponSoldOutException(CouponErrorCode.COUPON_SOLD_OUT);
            }
        }

        // Redis 기반 응답 반환 (id=null, DB 저장 전)
        return IssueCouponResponse.fromRedis(userId, couponEventId, couponEvent);
    }
}
