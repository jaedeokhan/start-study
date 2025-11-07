package com.ecommerce.domain.user;

import com.ecommerce.domain.point.exception.InsufficientPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 도메인 엔티티 테스트")
class UserTest {

    @Test
    @DisplayName("사용자 생성 시 포인트가 음수이면 예외 발생")
    void createUserWithNegativePoint() {
        // when & then
        assertThatThrownBy(() -> new User(1L, "사용자", -1000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("포인트 잔액은 0 이상이어야 합니다.");
    }

    @Test
    @DisplayName("포인트가 충분하면 true 반환")
    void hasPointReturnsTrue() {
        // given
        User user = new User(1L, "사용자", 10000);

        // when & then
        assertThat(user.hasPoint(5000)).isTrue();
        assertThat(user.hasPoint(10000)).isTrue();
    }

    @Test
    @DisplayName("포인트가 부족하면 false 반환")
    void hasPointReturnsFalse() {
        // given
        User user = new User(1L, "사용자", 10000);

        // when & then
        assertThat(user.hasPoint(10001)).isFalse();
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePointSuccess() {
        // given
        User user = new User(1L, "사용자", 10000);

        // when
        user.chargePoint(5000);

        // then
        assertThat(user.getPointBalance()).isEqualTo(15000);
    }

    @Test
    @DisplayName("충전 금액이 0 이하이면 예외 발생")
    void chargePointWithInvalidAmount() {
        // given
        User user = new User(1L, "사용자", 10000);

        // when & then
        assertThatThrownBy(() -> user.chargePoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePointSuccess() {
        // given
        User user = new User(1L, "사용자", 10000);

        // when
        user.usePoint(3000);

        // then
        assertThat(user.getPointBalance()).isEqualTo(7000);
    }

    @Test
    @DisplayName("포인트 부족 시 사용 실패")
    void usePointFail() {
        // given
        User user = new User(1L, "사용자", 10000);

        // when & then
        assertThatThrownBy(() -> user.usePoint(10001))
            .isInstanceOf(InsufficientPointException.class)
            .hasMessageContaining("포인트가 부족");
    }
}
