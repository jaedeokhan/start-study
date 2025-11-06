package com.ecommerce.presentation.dto.coupon;

import com.ecommerce.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "쿠폰 이벤트 목록 응답")
public class CouponEventListResponse {

    @Schema(description = "쿠폰 이벤트 목록")
    private List<CouponEventInfo> events;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "쿠폰 이벤트 정보")
    public static class CouponEventInfo {

        @Schema(description = "쿠폰 이벤트 ID", example = "10")
        private Long couponEventId;

        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        private String name;

        @Schema(description = "할인 유형", example = "AMOUNT")
        private DiscountType discountType;

        @Schema(description = "할인 금액 (고정 금액 할인인 경우)", example = "10000")
        private Long discountAmount;

        @Schema(description = "할인율 (비율 할인인 경우)", example = "10")
        private Integer discountRate;

        @Schema(description = "최대 할인 금액 (비율 할인인 경우)", example = "50000")
        private Long maxDiscountAmount;

        @Schema(description = "총 수량", example = "1000")
        private Integer totalQuantity;

        @Schema(description = "남은 수량", example = "450")
        private Integer remainingQuantity;

        @Schema(description = "시작일", example = "2025-10-29T00:00:00")
        private LocalDateTime startDate;

        @Schema(description = "종료일", example = "2025-11-30T23:59:59")
        private LocalDateTime endDate;
    }
}
