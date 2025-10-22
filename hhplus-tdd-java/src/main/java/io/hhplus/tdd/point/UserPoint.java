package io.hhplus.tdd.point;

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
        if (chargeAmount <= 0) {
            throw new UserPointInputValidException("충전 금액은 0보다 커야 합니다.");
        }
        return new UserPoint(this.id, this.point + chargeAmount, System.currentTimeMillis());
    }

    public UserPoint use(long useAmount) {
        if (useAmount <= 0) {
            throw new UserPointInputValidException("사용 금액은 0보다 커야 합니다.");
        }
        return new UserPoint(this.id, this.point - useAmount, System.currentTimeMillis());
    }
}
