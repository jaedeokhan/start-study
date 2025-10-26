package io.hhplus.tdd.common.exception;

import io.hhplus.tdd.common.constants.CommonConstants;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    MIN_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "금액은 " + CommonConstants.MIN_AMOUNT + "보다 커야 합니다."),
    MAX_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "금액은 " + CommonConstants.MAX_AMOUNT + "보다 작아야 합니다."),
    UNIT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "금액은 " + CommonConstants.UNIT_AMOUNT + "원 단위로만 입력 가능합니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "사용할 수 있는 금액이 부족합니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
