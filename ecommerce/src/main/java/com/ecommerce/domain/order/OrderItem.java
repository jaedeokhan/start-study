package com.ecommerce.domain.order;

import lombok.Getter;

/**
 * 주문 항목 Entity
 * - 주문 내 개별 상품 정보
 */
@Getter
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private int quantity;
    private long price;          // 주문 시점의 상품 가격
    private long subtotal;       // 소계 (price * quantity)

    // 생성자
    public OrderItem(Long id, Long orderId, Long productId,
                     String productName, int quantity, long price) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }

        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = price * quantity;
    }
}
