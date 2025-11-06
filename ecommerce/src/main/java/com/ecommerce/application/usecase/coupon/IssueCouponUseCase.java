package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import com.ecommerce.domain.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * US-COUP-002: 쿠폰 발급
 */
@Component
@RequiredArgsConstructor
public class IssueCouponUseCase {
    private final CouponEventRepository couponEventRepository;
    private final UserCouponRepository userCouponRepository;

    public IssueCouponResponse execute(Long couponEventId, Long userId) {
        // 1. 쿠폰 이벤트 조회
        CouponEvent couponEvent = couponEventRepository.findById(couponEventId)
            .orElseThrow(() -> new CouponEventNotFoundException("쿠폰 이벤트를 찾을 수 없습니다: " + couponEventId));

        // 2. 쿠폰 발급 가능 여부 검증 (Entity 비즈니스 로직)
        LocalDateTime now = LocalDateTime.now();
        if (!couponEvent.isAvailable(now)) {
            throw new CouponExpiredException("쿠폰 발급 기간이 아닙니다.");
        }

        // 3. 중복 발급 체크
        if (userCouponRepository.existsByUserIdAndCouponEventId(userId, couponEventId)) {
            throw new CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.");
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
