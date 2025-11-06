package com.ecommerce.presentation.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장바구니 조회 응답")
public class CartResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "장바구니 상품 목록")
    private List<CartItem> items;

    @Schema(description = "총 금액", example = "3030000")
    private Long totalAmount;

    @Schema(description = "총 상품 개수", example = "2")
    private Integer totalItems;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "장바구니 상품 정보")
    public static class CartItem {
        @Schema(description = "장바구니 항목 ID", example = "1")
        private Long cartItemId;

        @Schema(description = "상품 ID", example = "1")
        private Long productId;

        @Schema(description = "상품명", example = "노트북")
        private String productName;

        @Schema(description = "상품 가격", example = "1500000")
        private Long price;

        @Schema(description = "수량", example = "2")
        private Integer quantity;

        @Schema(description = "소계", example = "3000000")
        private Long subtotal;

        @Schema(description = "재고", example = "50")
        private Integer stock;
    }
}
