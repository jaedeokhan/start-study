package api.swagger.dto.coupon;

import api.swagger.enums.CouponStatus;
import api.swagger.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "쿠폰 응답")
public class IssueCouponResponse {

    @Schema(description = "ID", example = "123")
    private Long id;

    @Schema(description = "쿠폰 이벤트 ID", example = "10")
    private Long couponEventId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

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

    @Schema(description = "상태(AVAILABLE, USED, EXPIRED)", example = "AVAILABLE")
    private CouponStatus status;

    @Schema(description = "발급 시간", example = "2025-10-29T14:30:00")
    private LocalDateTime issuedAt;

    @Schema(description = "사용 시간", example = "2025-10-29T00:00:00")
    private LocalDateTime usedAt;

    @Schema(description = "만료 시간", example = "2025-11-30T23:59:59")
    private LocalDateTime expiresAt;
}
