package com.ecommerce.presentation.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이지네이션 정보")
public class PaginationInfo {

    @Schema(description = "현재 페이지 번호", example = "0")
    private int currentPage;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "전체 요소 수", example = "100")
    private long totalElements;

    @Schema(description = "페이지 크기", example = "20")
    private int size;
}
