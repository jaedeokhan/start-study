package com.ecommerce.domain.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외의 기본 클래스
 * - 모든 도메인 예외는 이 클래스를 상속받아야 함
 * - ErrorCode를 통해 에러 정보를 관리
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public BaseException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    /**
     * 최종 메시지 반환 (커스텀 메시지 우선)
     */
    public String getDisplayMessage() {
        return customMessage != null ? customMessage : errorCode.getMessage();
    }
}
