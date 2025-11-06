package com.ecommerce.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 상태")
public enum CouponStatus {
    @Schema(description = "사용 가능")
    AVAILABLE,

    @Schema(description = "사용됨")
    USED,

    @Schema(description = "만료됨")
    EXPIRED
}
