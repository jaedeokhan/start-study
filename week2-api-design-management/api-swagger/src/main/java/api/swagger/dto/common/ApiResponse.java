package api.swagger.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API 응답 공통 구조")
public class ApiResponse<T> {

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "응답 시간", example = "2025-10-29T14:30:00")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, LocalDateTime.now());
    }
}
