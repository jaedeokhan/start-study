package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import com.ecommerce.domain.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * US-COUP-002: 쿠폰 발급
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final CouponEventRepository couponEventRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public IssueCouponResponse execute(Long couponEventId, Long userId) {
        // 1. 쿠폰 이벤트 조회
        CouponEvent couponEvent = couponEventRepository.findById(couponEventId)
            .orElseThrow(() -> new CouponEventNotFoundException(CouponErrorCode.COUPON_EVENT_NOT_FOUND));

        // 2. 쿠폰 발급 가능 여부 검증 (Entity 비즈니스 로직)
        LocalDateTime now = LocalDateTime.now();
        if (!couponEvent.isAvailable(now)) {
            throw new CouponExpiredException(CouponErrorCode.COUPON_EXPIRED);
        }

        // 3. 중복 발급 체크
        if (userCouponRepository.existsByUserIdAndCouponEventId(userId, couponEventId)) {
            throw new CouponAlreadyIssuedException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 4. 쿠폰 발급 (동시성 제어는 Repository에서)
        couponEventRepository.issueCoupon(couponEventId);


        // 5. 사용자 쿠폰 생성
        UserCoupon userCoupon = new UserCoupon(
            null,
            userId,
            couponEventId,
            couponEvent.getStartDate(),
            couponEvent.getEndDate()
        );
        userCoupon = userCouponRepository.save(userCoupon);

        // 6. 응답 생성
        return IssueCouponResponse.from(userCoupon, couponEvent);
    }
}
