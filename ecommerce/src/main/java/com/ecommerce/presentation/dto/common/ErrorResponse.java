package com.ecommerce.presentation.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "에러 정보")
    private ErrorDetail error;

    @Schema(description = "에러 발생 시간", example = "2025-10-29T14:30:00")
    private LocalDateTime timestamp;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "에러 상세 정보")
    public static class ErrorDetail {
        @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
        private String code;

        @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
        private String message;

        @Schema(description = "에러 상세 설명 (optional)", example = "userId: 1")
        private String details;
    }
}
