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

    @ParameterizedTest(name = "{0}원 충전 시 포인트 증가")
    @ValueSource(ints = {1200, 1500})
    @DisplayName("포인트 충전 성공")
    void chargePoint_IncreasesBalance(int chargeAmount) {
        // given
        UserPoint userPoint = UserPoint.empty(1L);

        // when
        UserPoint chargedUserPoint = userPoint.charge(chargeAmount);

        // then
        assertThat(chargedUserPoint.point()).isEqualTo(chargeAmount);
    }

    @ParameterizedTest(name = "{0}원 사용 시 포인트 감소")
    @ValueSource(ints = {2000, 3000})
    @DisplayName("포인트 사용 성공")
    void usePoint_DecreasesBalance(int useAmount) {
        // given
        UserPoint userPoint = new UserPoint(1L, useAmount, System.currentTimeMillis());

        // when
        UserPoint usedUserPoint = userPoint.use(useAmount);

        // then
        assertThat(usedUserPoint.point()).isEqualTo(0);
    }

    @ParameterizedTest(name = "{0}원 충전 시 예외 발생")
    @ValueSource(ints = {-1200, -1500})
    @DisplayName("충전 금액이 최소값 이하일 때 예외 발생")
    void chargePoint_ThrowsException_WhenAmountBelowMinimum(int chargeAmount) {
        UserPoint userPoint = UserPoint.empty(1L);

        assertThatThrownBy(() -> {
            userPoint.charge(chargeAmount);
        })
        .isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.MIN_AMOUNT_INVALID.getMessage());
    }

    @ParameterizedTest(name = "{0}원 사용 시 예외 발생")
    @ValueSource(ints = {-2000, -3000})
    @DisplayName("사용 금액이 최소값 이하일 때 예외 발생")
    void usePoint_ThrowsException_WhenAmountBelowMinimum(int useAmount) {
        UserPoint userPoint = new UserPoint(1L, useAmount, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(useAmount);
        })
        .isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.MIN_AMOUNT_INVALID.getMessage());
    }

    @ParameterizedTest(name = "{0}원 사용 시 예외 발생")
    @ValueSource(ints = {1_000_001, 1_000_002})
    @DisplayName("사용 금액이 최대값 초과 시 예외 발생")
    void usePoint_ThrowsException_WhenAmountExceedsMaximum(int amount) {
        UserPoint userPoint = new UserPoint(1L, amount, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(amount);
        })
        .isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.MAX_AMOUNT_INVALID.getMessage());
    }

    @ParameterizedTest(name = "{0}원 사용 시 예외 발생")
    @ValueSource(ints = {101, 1012})
    @DisplayName("사용 금액이 100원 단위가 아닐 때 예외 발생")
    void usePoint_ThrowsException_WhenAmountNotInValidUnit(int amount) {
        UserPoint userPoint = new UserPoint(1L, amount, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(amount);
        }).isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.UNIT_AMOUNT_INVALID.getMessage());
    }

    @Test
    @DisplayName("사용 금액이 포인트를 초과할 때 예외 발생")
    void usePoint_ThrowsException_WhenAmountExceedsBalance() {
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());

        assertThatThrownBy(() -> {
            userPoint.use(1100);
        }).isInstanceOf(UserPointInputValidException.class)
        .hasMessageContaining(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
    }


}
