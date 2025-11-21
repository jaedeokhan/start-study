package com.ecommerce.presentation.dto.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.coupon.UserCoupon;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "쿠폰 응답")
public record IssueCouponResponse (

    @Schema(description = "ID", example = "123")
    Long id,

    @Schema(description = "쿠폰 이벤트 ID", example = "10")
    Long couponEventId,

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
    String couponName,

    @Schema(description = "할인 유형", example = "AMOUNT")
    DiscountType discountType,

    @Schema(description = "할인 금액 (고정 금액 할인인 경우)", example = "10000")
    Long discountAmount,

    @Schema(description = "할인율 (비율 할인인 경우)", example = "10")
    Integer discountRate,

    @Schema(description = "최대 할인 금액 (비율 할인인 경우)", example = "50000")
    Long maxDiscountAmount,

    @Schema(description = "상태(AVAILABLE, USED, EXPIRED)", example = "AVAILABLE")
    CouponStatus status,

    @Schema(description = "발급 시간", example = "2025-10-29T14:30:00")
    LocalDateTime issuedAt,

    @Schema(description = "사용 시간", example = "2025-10-29T00:00:00")
    LocalDateTime usedAt,

    @Schema(description = "만료 시간", example = "2025-11-30T23:59:59")
    LocalDateTime expiresAt
) {
    public static IssueCouponResponse from(UserCoupon userCoupon, CouponEvent couponEvent) {
        return new IssueCouponResponse(
            userCoupon.getId(),
            couponEvent.getId(),
            userCoupon.getUserId(),
            couponEvent.getName(),
            couponEvent.getDiscountType(),
            couponEvent.getDiscountType() == DiscountType.AMOUNT ? couponEvent.getDiscountAmount() : null,
            couponEvent.getDiscountType() == DiscountType.RATE ? couponEvent.getDiscountRate() : null,
            couponEvent.getDiscountType() == DiscountType.RATE ? (long) couponEvent.getMaxDiscountAmount() : null,
            userCoupon.getStatus(couponEvent, LocalDateTime.now()),
            userCoupon.getIssuedAt(),
            userCoupon.getUsedAt(),
            userCoupon.getEndDate()
        );
    }
}
