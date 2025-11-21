package com.ecommerce.presentation.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "장바구니 상품 수량 변경 요청")
public record UpdateCartItemRequest (

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    @Schema(description = "변경할 수량", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer quantity
) {}
