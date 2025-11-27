package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.presentation.dto.coupon.CouponEventListResponse;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * US-COUP-001: 사용 가능한 쿠폰 이벤트 목록 조회
 */
@Component
@RequiredArgsConstructor
public class GetCouponEventsUseCase {
    private final CouponEventRepository couponEventRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "coupon:event")
    public CouponEventListResponse execute() {
        // 1. 모든 쿠폰 이벤트 조회
        List<CouponEvent> allEvents = couponEventRepository.findAll();

        // 2. 현재 진행 중인 이벤트만 필터링
        LocalDateTime now = LocalDateTime.now();
        List<CouponEvent> couponEvents = allEvents.stream()
            .filter(event -> event.isAvailable(now))
            .collect(java.util.stream.Collectors.toList());

        // 3. 응답 생성
        return CouponEventListResponse.from(couponEvents);
    }
}
