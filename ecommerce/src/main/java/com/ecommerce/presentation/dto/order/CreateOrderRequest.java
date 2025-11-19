package com.ecommerce.presentation.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest (

    @Schema(description = "사용자 ID", example = "1", required = true)
    Long userId,

    @Schema(description = "쿠폰 ID (없으면 할인 미적용)", example = "5")
    Long couponId
){}