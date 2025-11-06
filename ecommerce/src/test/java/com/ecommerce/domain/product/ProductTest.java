package com.ecommerce.domain.product;

import com.ecommerce.domain.product.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product 도메인 엔티티 테스트")
class ProductTest {

    @Test
    @DisplayName("상품 생성 시 가격이 0 이하이면 예외 발생")
    void createProductWithInvalidPrice() {
        // when & then
        assertThatThrownBy(() -> new Product(1L, "상품", "설명", 0, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("가격은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("상품 생성 시 재고가 음수이면 예외 발생")
    void createProductWithNegativeStock() {
        // when & then
        assertThatThrownBy(() -> new Product(1L, "상품", "설명", 1000, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("재고는 0 이상이어야 합니다.");
    }

    @Test
    @DisplayName("재고가 충분하면 true 반환")
    void hasStockReturnsTrue() {
        // given
        Product product = new Product(1L, "상품", "설명", 1000, 100);

        // when & then
        assertThat(product.hasStock(50)).isTrue();
        assertThat(product.hasStock(100)).isTrue();
    }

    @Test
    @DisplayName("재고가 부족하면 false 반환")
    void hasStockReturnsFalse() {
        // given
        Product product = new Product(1L, "상품", "설명", 1000, 100);

        // when & then
        assertThat(product.hasStock(101)).isFalse();
    }

    @Test
    @DisplayName("재고 차감 성공")
    void decreaseStockSuccess() {
        // given
        Product product = new Product(1L, "상품", "설명", 1000, 100);

        // when
        product.decreaseStock(30);

        // then
        assertThat(product.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 부족 시 차감 실패")
    void decreaseStockFail() {
        // given
        Product product = new Product(1L, "상품", "설명", 1000, 50);

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(51))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("재고 복구 성공")
    void increaseStockSuccess() {
        // given
        Product product = new Product(1L, "상품", "설명", 1000, 50);

        // when
        product.increaseStock(20);

        // then
        assertThat(product.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 복구 시 0 이하 수량이면 예외 발생")
    void increaseStockWithInvalidQuantity() {
        // given
        Product product = new Product(1L, "상품", "설명", 1000, 50);

        // when & then
        assertThatThrownBy(() -> product.increaseStock(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("복구 수량은 0보다 커야 합니다.");
    }
}
