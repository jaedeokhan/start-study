package com.ecommerce.presentation.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "에러 응답")
public record ErrorResponse (

    @Schema(description = "에러 정보")
    ErrorDetail error,

    @Schema(description = "에러 발생 시간", example = "2025-10-29T14:30:00")
    LocalDateTime timestamp
) {
    @Schema(description = "에러 상세 정보")
    public record ErrorDetail (
        @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
        String code,

        @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
        String message,

        @Schema(description = "에러 상세 설명 (optional)", example = "userId: 1")
        String details
    ) {}
}
