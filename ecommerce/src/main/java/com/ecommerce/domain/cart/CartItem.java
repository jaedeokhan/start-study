package com.ecommerce.domain.cart;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 장바구니 아이템 Entity
 * - 수량 관리 비즈니스 로직 포함
 */
@Getter
public class CartItem {
    private Long id;
    private Long userId;
    private Long productId;
    private int quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자
    public CartItem(Long id, Long userId, Long productId, int quantity) {
        validateQuantity(quantity);

        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 검증 로직 (Entity 내부) ==========

    /**
     * 수량 검증
     */
    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 수량 증가 (동일 상품 추가 시)
     * @param amount 증가할 수량
     */
    public void increaseQuantity(int amount) {
        validateQuantity(amount);
        this.quantity += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수량 변경
     * @param newQuantity 새로운 수량
     */
    public void updateQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }
}
