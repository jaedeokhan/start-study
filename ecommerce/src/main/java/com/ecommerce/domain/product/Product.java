package com.ecommerce.domain.product;

import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 Entity
 * - 재고 관리 비즈니스 로직 포함
 */
@Getter
public class Product {
    private Long id;
    private String name;
    private String description;
    private long price;
    private int stock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자
    public Product(Long id, String name, String description, long price, int stock) {
        validatePrice(price);
        validateStock(stock);

        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 검증 로직 (Entity 내부) ==========

    /**
     * 가격 검증
     */
    private void validatePrice(long price) {
        if (price <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
        }
    }

    /**
     * 재고 검증
     */
    private void validateStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 재고 확인 (차감하지 않음)
     * @param quantity 요청 수량
     * @return 재고 충분 여부
     */
    public boolean hasStock(int quantity) {
        return this.stock >= quantity;
    }

    /**
     * 재고 차감
     * @param quantity 차감할 수량
     * @throws InsufficientStockException 재고 부족 시
     */
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new InsufficientStockException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 복구 (주문 취소 시 사용)
     * @param quantity 복구할 수량
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복구 수량은 0보다 커야 합니다.");
        }
        this.stock += quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
