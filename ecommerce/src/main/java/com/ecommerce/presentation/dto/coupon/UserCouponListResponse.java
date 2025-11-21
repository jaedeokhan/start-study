package com.ecommerce.presentation.dto.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.domain.coupon.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "사용자 쿠폰 목록 응답")
public record UserCouponListResponse (

    @Schema(description = "쿠폰 목록")
    List<UserCoupon> coupons,

    @Schema(description = "쿠폰 개수 요약")
    CouponSummary summary
) {
    @Schema(description = "사용자 쿠폰")
    public record UserCoupon (

        @Schema(description = "id", example = "123")
        Long id,

        @Schema(description = "쿠폰 이벤트 ID", example = "10")
        Long couponEventId,

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

        @Schema(description = "쿠폰 상태", example = "AVAILABLE")
        CouponStatus status,

        @Schema(description = "발급 시간", example = "2025-10-29T14:30:00")
        LocalDateTime issuedAt,

        @Schema(description = "사용 시간", example = "2025-10-29T00:00:00")
        LocalDateTime usedAt,

        @Schema(description = "만료 시간", example = "2025-11-30T23:59:59")
        LocalDateTime expiresAt
    ){}

    @Schema(description = "쿠폰 개수 요약")
    public record CouponSummary (

        @Schema(description = "전체 쿠폰 수", example = "5")
        Integer totalCount,

        @Schema(description = "사용 가능한 쿠폰 수", example = "2")
        Integer availableCount,

        @Schema(description = "사용된 쿠폰 수", example = "2")
        Integer usedCount,

        @Schema(description = "만료된 쿠폰 수", example = "1")
        Integer expiredCount
    ) {}
    public static UserCouponListResponse from(List<com.ecommerce.domain.coupon.UserCoupon> domainUserCoupons,
                                              Map<Long, CouponEvent> couponEventMap,
                                              LocalDateTime now) {
        // 쿠폰 목록 변환
        List<UserCoupon> coupons = domainUserCoupons.stream()
            .map(userCoupon -> {
                CouponEvent event = couponEventMap.get(userCoupon.getCouponEventId());
                CouponStatus status = userCoupon.getStatus(event, now);

                return new UserCoupon(
                    userCoupon.getId(),
                    event.getId(),
                    event.getName(),
                    event.getDiscountType(),
                    event.getDiscountType() == DiscountType.AMOUNT ? event.getDiscountAmount() : null,
                    event.getDiscountType() == DiscountType.RATE ? event.getDiscountRate() : null,
                    event.getDiscountType() == DiscountType.RATE ? (long) event.getMaxDiscountAmount() : null,
                    status,
                    userCoupon.getIssuedAt(),
                    userCoupon.getUsedAt(),
                    userCoupon.getEndDate()
                );
            })
            .collect(Collectors.toList());

        // 쿠폰 개수 요약
        int totalCount = coupons.size();
        int availableCount = (int) coupons.stream().filter(c -> c.status() == CouponStatus.AVAILABLE).count();
        int usedCount = (int) coupons.stream().filter(c -> c.status() == CouponStatus.USED).count();
        int expiredCount = (int) coupons.stream().filter(c -> c.status() == CouponStatus.EXPIRED).count();

        CouponSummary summary = new CouponSummary(totalCount, availableCount, usedCount, expiredCount);

        return new UserCouponListResponse(coupons, summary);
    }
}
