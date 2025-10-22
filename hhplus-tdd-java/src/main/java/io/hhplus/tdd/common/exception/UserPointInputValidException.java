package io.hhplus.tdd.common.exception;

import org.springframework.http.HttpStatus;

public class UserPointInputValidException extends RuntimeException {
    private final String code;

    public UserPointInputValidException(String message) {
        super(message);
        this.code = String.valueOf(HttpStatus.BAD_REQUEST.value()); // default error code : 400
    }

    public UserPointInputValidException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
