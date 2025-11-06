package com.ecommerce.presentation.controller;

import com.ecommerce.presentation.api.CouponApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.coupon.CouponEventListResponse;
import com.ecommerce.presentation.dto.coupon.UserCouponListResponse;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import com.ecommerce.presentation.dto.coupon.IssueCouponRequest;
import com.ecommerce.enums.CouponStatus;
import com.ecommerce.enums.DiscountType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CouponController implements CouponApi {

    @PostMapping("/coupons/{couponEventId}/issue")
    @Override
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            @PathVariable Long couponEventId,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        IssueCouponResponse data = new IssueCouponResponse(
                123L,
                10L,
                1L,
                "신규 가입 쿠폰",
                DiscountType.AMOUNT,
                10000L,
                null,
                null,
                CouponStatus.AVAILABLE,
                LocalDateTime.of(2025, 10, 29, 0, 0),
                LocalDateTime.of(2025, 11, 30, 23, 59, 59),
                LocalDateTime.of(2025, 10, 29, 14, 30)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @GetMapping("/coupons")
    @Override
    public ResponseEntity<ApiResponse<UserCouponListResponse>> getCoupons(
            @RequestParam Long userId,
            @RequestParam(required = false) CouponStatus status
    ) {
        UserCouponListResponse data = new UserCouponListResponse(
                List.of(
                        new UserCouponListResponse.UserCoupon(
                                123L,
                                10L,
                                "신규 가입 쿠폰",
                                DiscountType.AMOUNT,
                                10000L,
                                null,
                                null,
                                CouponStatus.AVAILABLE,
                                LocalDateTime.of(2025, 10, 29, 0, 0),
                                LocalDateTime.of(2025, 11, 30, 23, 59, 59),
                                LocalDateTime.of(2025, 10, 29, 14, 30)
                        ),
                        new UserCouponListResponse.UserCoupon(
                                124L,
                                11L,
                                "첫 구매 10% 할인",
                                DiscountType.RATE,
                                null,
                                10,
                                50000L,
                                CouponStatus.AVAILABLE,
                                LocalDateTime.of(2025, 10, 1, 0, 0),
                                LocalDateTime.of(2025, 10, 31, 23, 59, 59),
                                LocalDateTime.of(2025, 10, 15, 10, 0)
                        )
                ),
                new UserCouponListResponse.CouponSummary(5, 2, 2, 1)
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/coupons-events")
    @Override
    public ResponseEntity<ApiResponse<CouponEventListResponse>> getCouponEvents() {
        CouponEventListResponse data = new CouponEventListResponse(
                List.of(
                        new CouponEventListResponse.CouponEventInfo(
                                10L,
                                "신규 가입 쿠폰",
                                DiscountType.AMOUNT,
                                10000L,
                                null,
                                null,
                                1000,
                                450,
                                LocalDateTime.of(2025, 10, 29, 0, 0),
                                LocalDateTime.of(2025, 11, 30, 23, 59, 59)
                        ),
                        new CouponEventListResponse.CouponEventInfo(
                                11L,
                                "첫 구매 10% 할인",
                                DiscountType.RATE,
                                null,
                                10,
                                50000L,
                                500,
                                120,
                                LocalDateTime.of(2025, 10, 1, 0, 0),
                                LocalDateTime.of(2025, 10, 31, 23, 59, 59)
                        )
                )
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
