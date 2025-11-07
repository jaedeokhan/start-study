package com.ecommerce.presentation.exception;

import com.ecommerce.domain.common.exception.BaseException;
import com.ecommerce.domain.common.exception.ErrorCode;
import com.ecommerce.presentation.dto.common.ErrorResponse;
import com.ecommerce.presentation.dto.common.ErrorResponse.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * - 모든 Controller에서 발생하는 예외를 통합 처리
 * - 일관된 에러 응답 형식 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 통합 처리
     * - BaseException을 상속받는 모든 도메인 예외를 처리
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = ex.getDisplayMessage(); // 커스텀 메시지 우선 사용
        log.error("{}: {}", errorCode.getCode(), message);

        ErrorDetail errorDetail = new ErrorDetail(
            errorCode.getCode(),
            message,
            null
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorDetail, LocalDateTime.now()));
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getAllErrors().stream()
            .map(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                return fieldName + ": " + errorMessage;
            })
            .collect(Collectors.joining(", "));

        log.error("Validation error: {}", details);

        ErrorDetail errorDetail = new ErrorDetail(
            "VALIDATION_ERROR",
            "입력 값 검증 실패",
            details
        );

        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(errorDetail, LocalDateTime.now()));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());

        ErrorDetail errorDetail = new ErrorDetail(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            null
        );

        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(errorDetail, LocalDateTime.now()));
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ErrorDetail errorDetail = new ErrorDetail(
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다.",
            null
        );

        return ResponseEntity
            .internalServerError()
            .body(new ErrorResponse(errorDetail, LocalDateTime.now()));
    }
}
