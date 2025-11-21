package com.ecommerce.presentation.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "API 응답 공통 구조")
public record ApiResponse<T> (

    @Schema(description = "성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 데이터")
    T data,

    @Schema(description = "에러 코드 (에러 시)")
    String errorCode,

    @Schema(description = "에러 메시지 (에러 시)")
    String errorMessage,

    @Schema(description = "응답 시간", example = "2025-10-29T14:30:00")
    LocalDateTime timestamp
) {
    // 성공 응답
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(true, data, null, null, LocalDateTime.now());
    }

    // 에러 응답 (데이터 없음)
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, null, errorCode, errorMessage, LocalDateTime.now());
    }

    // 에러 응답 (데이터 포함 - validation errors 등)
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, T data) {
        return new ApiResponse<>(false, data, errorCode, errorMessage, LocalDateTime.now());
    }
}
