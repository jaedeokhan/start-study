package io.hhplus.tdd.point;

import io.hhplus.tdd.common.constants.CommonConstants;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserPointInputValidException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long chargeAmount) {
        validateAmount(chargeAmount);
        return new UserPoint(this.id, this.point + chargeAmount, System.currentTimeMillis());
    }

    public UserPoint use(long useAmount) {
        validateAmount(useAmount);
        validateBalance(useAmount);
        return new UserPoint(this.id, this.point - useAmount, System.currentTimeMillis());
    }

    /**
     * 최소, 최대, 100의 자리 검증하는 함수
     * @param amount
     */
    private void validateAmount(long amount) {
        if (amount <= CommonConstants.MIN_AMOUNT) {
            throw new UserPointInputValidException(ErrorCode.MIN_AMOUNT_INVALID.getMessage());
        }
        if (amount > CommonConstants.MAX_AMOUNT) {
            throw new UserPointInputValidException(ErrorCode.MAX_AMOUNT_INVALID.getMessage());
        }
        if (amount % CommonConstants.UNIT_AMOUNT != 0) {
            throw new UserPointInputValidException(ErrorCode.UNIT_AMOUNT_INVALID.getMessage());
        }
    }

    /**
     * 사용금액의 잔고를 검증하는 함수
     * @param useAmount
     */
    private void validateBalance(long useAmount) {
        if (this.point < useAmount) {
            throw new UserPointInputValidException(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
        }
    }
}
