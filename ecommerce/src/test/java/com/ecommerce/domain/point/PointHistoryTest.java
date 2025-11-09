package com.ecommerce.domain.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PointHistory 도메인 엔티티 테스트")
class PointHistoryTest {

    @Test
    @DisplayName("포인트 충전 이력 생성 성공")
    void createChargeHistory() {
        // given & when
        PointHistory history = new PointHistory(
            1L,
            1L,
            10000L,
            TransactionType.CHARGE,
            110000L,
            "포인트 충전"
        );

        // then
        assertThat(history.getId()).isEqualTo(1L);
        assertThat(history.getUserId()).isEqualTo(1L);
        assertThat(history.getPointAmount()).isEqualTo(10000L);
        assertThat(history.getTransactionType()).isEqualTo(TransactionType.CHARGE);
        assertThat(history.getBalanceAfter()).isEqualTo(110000L);
        assertThat(history.getOrderId()).isNull();
        assertThat(history.getDescription()).isEqualTo("포인트 충전");
    }

    @Test
    @DisplayName("포인트 사용 이력 생성 성공 (주문 ID 포함)")
    void createUseHistory() {
        // given & when
        PointHistory history = new PointHistory(
            2L,
            1L,
            -50000L,
            TransactionType.USE,
            60000L,
            100L,
            "주문 결제"
        );

        // then
        assertThat(history.getId()).isEqualTo(2L);
        assertThat(history.getUserId()).isEqualTo(1L);
        assertThat(history.getPointAmount()).isEqualTo(-50000L);
        assertThat(history.getTransactionType()).isEqualTo(TransactionType.USE);
        assertThat(history.getBalanceAfter()).isEqualTo(60000L);
        assertThat(history.getOrderId()).isEqualTo(100L);
        assertThat(history.getDescription()).isEqualTo("주문 결제");
    }

    @Test
    @DisplayName("충전 금액이 0 이하이면 예외 발생")
    void chargeWithInvalidAmount() {
        // when & then
        assertThatThrownBy(() -> new PointHistory(
            1L,
            1L,
            0L,
            TransactionType.CHARGE,
            100000L,
            "포인트 충전"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("사용 금액이 0 이상이면 예외 발생")
    void useWithInvalidAmount() {
        // when & then
        assertThatThrownBy(() -> new PointHistory(
            1L,
            1L,
            1000L,
            TransactionType.USE,
            100000L,
            100L,
            "주문 결제"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용 금액은 0보다 작아야 합니다.");
    }

    @Test
    @DisplayName("환불 금액이 0 이하이면 예외 발생")
    void refundWithInvalidAmount() {
        // when & then
        assertThatThrownBy(() -> new PointHistory(
            1L,
            1L,
            -1000L,
            TransactionType.REFUND,
            100000L,
            100L,
            "주문 취소"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("환불 금액은 0보다 커야 합니다.");
    }
}
