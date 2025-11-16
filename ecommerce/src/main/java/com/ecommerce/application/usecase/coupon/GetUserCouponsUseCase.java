package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.presentation.dto.coupon.UserCouponListResponse;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-COUP-003: 사용자가 보유한 쿠폰 목록 조회
 */
@Component
@RequiredArgsConstructor
public class GetUserCouponsUseCase {
    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;

    @Transactional(readOnly = true)
    public UserCouponListResponse execute(Long userId, CouponStatus status) {
        // 1. 사용자의 쿠폰 목록 조회
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        // 2. 쿠폰 이벤트 정보 조회
        List<Long> couponEventIds = userCoupons.stream()
            .map(UserCoupon::getCouponEventId)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, CouponEvent> couponEventMap = couponEventRepository.findAllById(couponEventIds)
                .stream()
                .filter(event -> couponEventIds.contains(event.getId()))
                .collect(Collectors.toMap(CouponEvent::getId, e -> e));

        // 3. 상태 필터링 (필요한 경우)
        LocalDateTime now = LocalDateTime.now();
        if (status != null) {
            userCoupons = userCoupons.stream()
                .filter(userCoupon -> {
                    CouponEvent event = couponEventMap.get(userCoupon.getCouponEventId());
                    return userCoupon.getStatus(event, now) == status;
                })
                .collect(Collectors.toList());
        }

        // 4. 응답 생성
        return UserCouponListResponse.from(userCoupons, couponEventMap, now);
    }
}
