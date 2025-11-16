package com.ecommerce.domain.cart;

import com.ecommerce.domain.common.exception.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_user_product",
                        columnNames = {"user_id", "product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    public CartItem(Long id, Long userId, Long productId, int quantity) {
        validateQuantity(quantity);

        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
    }

    // ========== 검증 로직 (Entity 내부) ==========

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
    }

    /**
     * 수량 변경
     * @param newQuantity 새로운 수량
     */
    public void updateQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }
}
