package com.ecommerce.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order 도메인 엔티티 테스트")
class OrderTest {

    @Test
    @DisplayName("주문 생성 - 쿠폰 없음")
    void createOrderWithoutCoupon() {
        // when
        Order order = new Order(1L, 1L, 10000, 0, 10000, null);

        // then
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getOriginalAmount()).isEqualTo(10000);
        assertThat(order.getDiscountAmount()).isEqualTo(0);
        assertThat(order.getFinalAmount()).isEqualTo(10000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("주문 생성 - 쿠폰 적용")
    void createOrderWithCoupon() {
        // when
        Order order = new Order(1L, 1L, 10000, 2000, 8000, 1L);

        // then
        assertThat(order.getOriginalAmount()).isEqualTo(10000);
        assertThat(order.getDiscountAmount()).isEqualTo(2000);
        assertThat(order.getFinalAmount()).isEqualTo(8000);
        assertThat(order.getCouponId()).isEqualTo(1L);
    }
}
