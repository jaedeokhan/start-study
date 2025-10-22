package io.hhplus.tdd.common.exception;

import io.hhplus.tdd.common.constants.CommonConstants;
import org.springframework.http.HttpStatus;

public enum ErrorCode {

    MIN_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, String.format("금액은 %d보다 커야 합니다.", CommonConstants.MIN_AMOUNT)),
    MAX_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, String.format("금액은 %d보다 작아야 합니다.", CommonConstants.MAX_AMOUNT)),
    UNIT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, String.format("금액은 %d원 단위로만 입력 가능합니다.", CommonConstants.UNIT_AMOUNT));

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
