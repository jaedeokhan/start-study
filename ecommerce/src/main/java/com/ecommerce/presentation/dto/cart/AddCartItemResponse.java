package com.ecommerce.presentation.dto.cart;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 상품 추가 응답")
public record AddCartItemResponse (

    @Schema(description = "장바구니 항목 ID", example = "1")
    Long cartItemId,

    @Schema(description = "상품 ID", example = "1")
    Long productId,

    @Schema(description = "상품명", example = "노트북")
    String productName,

    @Schema(description = "상품 가격", example = "1500000")
    Long price,

    @Schema(description = "수량", example = "2")
    Integer quantity,

    @Schema(description = "소계", example = "3000000")
    Long subtotal

) {
    public static AddCartItemResponse from(CartItem cartItem, Product product) {
        long subtotal = product.getPrice() * cartItem.getQuantity();

        return new AddCartItemResponse(
            cartItem.getId(),
            product.getId(),
            product.getName(),
            product.getPrice(),
            cartItem.getQuantity(),
            subtotal
        );
    }
}