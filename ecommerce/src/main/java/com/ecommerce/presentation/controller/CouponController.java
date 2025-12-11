package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.coupon.AsyncIssueCouponUseCase;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 쿠폰 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {
    private final GetCouponEventsUseCase getCouponEventsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final AsyncIssueCouponUseCase asyncIssueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;

    @Override
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            Long couponEventId,
            IssueCouponRequest request
    ) {
        IssueCouponResponse response = issueCouponUseCase.execute(couponEventId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<IssueCouponResponse>> asyncIssueCoupon(
            Long couponEventId,
            IssueCouponRequest request
    ) {
        IssueCouponResponse response = asyncIssueCouponUseCase.execute(couponEventId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<UserCouponListResponse>> getCoupons(
            Long userId,
            CouponStatus status
    ) {
        UserCouponListResponse response = getUserCouponsUseCase.execute(userId, status);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<CouponEventListResponse>> getCouponEvents() {
        CouponEventListResponse response = getCouponEventsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
