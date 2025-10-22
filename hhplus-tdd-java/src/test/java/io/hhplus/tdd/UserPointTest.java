package io.hhplus.tdd;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserPointInputValidException;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserPointTest {

    @ParameterizedTest(name = "{0}원 충전 시 금액 {0}원 증가한다.")
    @ValueSource(ints = {1200, 1500})
    @DisplayName("사용자 포인트가 충전되었을 때 포인트가 증가해야 한다.")
    void UserPoint_should_increasePoints_when_charged(int chargeAmount) {
        // given
        UserPoint userPoint = UserPoint.empty(1L);

        // when
        UserPoint chargedUserPoint = userPoint.charge(chargeAmount);

        // then
        assertThat(chargedUserPoint.point()).isEqualTo(chargeAmount);
    }

    @ParameterizedTest(name = "{0}원 사용 시 금액 {0}원 감소한다.")
    @ValueSource(ints = {2000, 3000})
    @DisplayName("사용자 포인트를 사용했을 때 포인트가 감소해야 한다.")
    void UserPoint_should_decreasePoints_when_used(int useAmount) {
        // given
        UserPoint userPoint = new UserPoint(1L, useAmount, System.currentTimeMillis());

        // when
        UserPoint usedUserPoint = userPoint.use(useAmount);

        // then
        assertThat(usedUserPoint.point()).isEqualTo(0);
    }

    @ParameterizedTest(name = "0보다 작은 {0}원 충전 시 예외가 발생한다.")
    @ValueSource(ints = {-1200, -1500})
    @DisplayName("사용자 충전 포인트가 음수일 때 예외가 발생한다.")
    void should_throwException_when_chargePointsAreNegative(int chargeAmount) {
        UserPoint userPoint = UserPoint.empty(1L);

        assertThatThrownBy(() -> {
            userPoint.charge(chargeAmount);
        })
        .isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.MIN_AMOUNT_INVALID.getMessage());
    }

    @ParameterizedTest(name = "0보다 작은 {0}원 사용 시 예외가 발생한다.")
    @ValueSource(ints = {-2000, -3000})
    @DisplayName("사용자 사용 포인트가 음수일 때 예외가 발생한다.")
    void should_throwException_when_usePointsAreNegative(int useAmount) {
        UserPoint userPoint = new UserPoint(1L, useAmount, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(useAmount);
        })
        .isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.MIN_AMOUNT_INVALID.getMessage());
    }

    @ParameterizedTest(name = "1_000_000 보다 큰 {0}원 사용 시 예외가 발생한다.")
    @ValueSource(ints = {1_000_001, 1_000_002})
    @DisplayName("사용자 사용 포인트가 1_000_000 보다 크면 예외가 발생한다.")
    void should_throwException_when_useAmountExceedsMaximum(int amount) {
        UserPoint userPoint = new UserPoint(1L, amount, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(amount);
        })
        .isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.MAX_AMOUNT_INVALID.getMessage());
    }

    @ParameterizedTest(name = "100원 단위가 아닌 {0}원 사용 시 예외가 발생한다.")
    @ValueSource(ints = {101, 1012})
    @DisplayName("사용자 사용 포인트가 1_000_000 보다 크면 예외가 발생한다.")
    void test(int amount) {
        UserPoint userPoint = new UserPoint(1L, amount, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(amount);
        }).isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.UNIT_AMOUNT_INVALID.getMessage());
    }
}
