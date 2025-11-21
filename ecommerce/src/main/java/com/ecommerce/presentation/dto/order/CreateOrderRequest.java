package com.ecommerce.presentation.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest (

    @Schema(description = "사용자 ID", example = "1", required = true)
    @NotNull(message = "사용자 ID는 필수입니다.")
    @Min(value = 1, message = "사용자 ID는 1 이상이어야 합니다.")
    Long userId,

    @Schema(description = "쿠폰 ID (없으면 할인 미적용)", example = "5")
    Long couponId
){}