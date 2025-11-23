package com.ecommerce.presentation.dto.cart;

import com.ecommerce.domain.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "장바구니 조회 응답")
public record CartResponse (

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "장바구니 상품 목록")
    List<CartItem> items,

    @Schema(description = "총 금액", example = "3030000")
    Long totalAmount,

    @Schema(description = "총 상품 개수", example = "2")
    Integer totalItems
    ){
        @Schema(description = "장바구니 상품 정보")
        public record CartItem (
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
            Long subtotal,

            @Schema(description = "재고", example = "50")
            Integer stock
        ) {}
    public static CartResponse from(Long userId, List<com.ecommerce.domain.cart.CartItem> domainCartItems, Map<Long, Product> productMap) {
        List<CartItem> items = domainCartItems.stream()
            .map(domainCartItem -> {
                Product product = productMap.get(domainCartItem.getProductId());
                long subtotal = product.getPrice() * domainCartItem.getQuantity();

                return new CartItem(
                    domainCartItem.getId(),
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    domainCartItem.getQuantity(),
                    subtotal,
                    product.getStock()
                );
            })
            .collect(Collectors.toList());

        long totalAmount = items.stream()
            .mapToLong(CartItem::subtotal)
            .sum();

        int totalItems = items.size();

        return new CartResponse(userId, items, totalAmount, totalItems);
    }
}
