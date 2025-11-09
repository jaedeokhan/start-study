package com.ecommerce.presentation.dto.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.domain.coupon.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 쿠폰 목록 응답")
public class UserCouponListResponse {

    @Schema(description = "쿠폰 목록")
    private List<UserCoupon> coupons;

    @Schema(description = "쿠폰 개수 요약")
    private CouponSummary summary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 쿠폰")
    public static class UserCoupon {

        @Schema(description = "id", example = "123")
        private Long id;

        @Schema(description = "쿠폰 이벤트 ID", example = "10")
        private Long couponEventId;

        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        private String couponName;

        @Schema(description = "할인 유형", example = "AMOUNT")
        private DiscountType discountType;

        @Schema(description = "할인 금액 (고정 금액 할인인 경우)", example = "10000")
        private Long discountAmount;

        @Schema(description = "할인율 (비율 할인인 경우)", example = "10")
        private Integer discountRate;

        @Schema(description = "최대 할인 금액 (비율 할인인 경우)", example = "50000")
        private Long maxDiscountAmount;

        @Schema(description = "쿠폰 상태", example = "AVAILABLE")
        private CouponStatus status;

        @Schema(description = "발급 시간", example = "2025-10-29T14:30:00")
        private LocalDateTime issuedAt;

        @Schema(description = "사용 시간", example = "2025-10-29T00:00:00")
        private LocalDateTime usedAt;

        @Schema(description = "만료 시간", example = "2025-11-30T23:59:59")
        private LocalDateTime expiresAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "쿠폰 개수 요약")
    public static class CouponSummary {

        @Schema(description = "전체 쿠폰 수", example = "5")
        private Integer totalCount;

        @Schema(description = "사용 가능한 쿠폰 수", example = "2")
        private Integer availableCount;

        @Schema(description = "사용된 쿠폰 수", example = "2")
        private Integer usedCount;

        @Schema(description = "만료된 쿠폰 수", example = "1")
        private Integer expiredCount;
    }

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
                    userCoupon.getValidUntil()
                );
            })
            .collect(Collectors.toList());

        // 쿠폰 개수 요약
        int totalCount = coupons.size();
        int availableCount = (int) coupons.stream().filter(c -> c.getStatus() == CouponStatus.AVAILABLE).count();
        int usedCount = (int) coupons.stream().filter(c -> c.getStatus() == CouponStatus.USED).count();
        int expiredCount = (int) coupons.stream().filter(c -> c.getStatus() == CouponStatus.EXPIRED).count();

        CouponSummary summary = new CouponSummary(totalCount, availableCount, usedCount, expiredCount);

        return new UserCouponListResponse(coupons, summary);
    }
}
