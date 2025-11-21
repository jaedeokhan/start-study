package com.ecommerce.presentation.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "쿠폰 발급 요청")
public record IssueCouponRequest (

    @NotNull
    @Positive
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long userId
){}
