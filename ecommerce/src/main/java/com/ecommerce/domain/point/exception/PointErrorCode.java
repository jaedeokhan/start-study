package com.ecommerce.domain.point.exception;

import com.ecommerce.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 포인트 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    INSUFFICIENT_POINT("INSUFFICIENT_POINT", "포인트가 부족합니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
