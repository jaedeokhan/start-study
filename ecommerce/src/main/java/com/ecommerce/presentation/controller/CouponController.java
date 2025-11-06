package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.coupon.GetCouponEventsUseCase;
import com.ecommerce.application.usecase.coupon.GetUserCouponsUseCase;
import com.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.presentation.api.CouponApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.coupon.CouponEventListResponse;
import com.ecommerce.presentation.dto.coupon.IssueCouponRequest;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import com.ecommerce.presentation.dto.coupon.UserCouponListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 쿠폰 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController implements CouponApi {
    // ✅ UseCase 주입
    private final GetCouponEventsUseCase getCouponEventsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;

    @PostMapping("/coupons/{couponEventId}/issue")
    @Override
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            @PathVariable Long couponEventId,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        IssueCouponResponse response = issueCouponUseCase.execute(couponEventId, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/coupons")
    @Override
    public ResponseEntity<ApiResponse<UserCouponListResponse>> getCoupons(
            @RequestParam Long userId,
            @RequestParam(required = false) CouponStatus status
    ) {
        UserCouponListResponse response = getUserCouponsUseCase.execute(userId, status);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/coupons-events")
    @Override
    public ResponseEntity<ApiResponse<CouponEventListResponse>> getCouponEvents() {
        CouponEventListResponse response = getCouponEventsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
