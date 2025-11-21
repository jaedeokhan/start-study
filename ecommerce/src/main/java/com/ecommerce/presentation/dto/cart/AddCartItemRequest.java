package com.ecommerce.presentation.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "장바구니 상품 추가 요청")
public record AddCartItemRequest (

    @NotNull(message = "사용자 ID는 필수입니다.")
    @Min(value = 1, message = "사용자 ID는 1 이상이어야 합니다.")
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long userId,

    @NotNull(message = "상품 ID는 필수입니다.")
    @Min(value = 1, message = "상품 ID는 1 이상이어야 합니다.")
    @Schema(description = "상품 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long productId,

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    @Schema(description = "수량", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer quantity
) {}