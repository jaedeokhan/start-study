package com.ecommerce.presentation.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지네이션 정보")
public record PaginationInfo (

    @Schema(description = "현재 페이지 번호", example = "0")
    int currentPage,

    @Schema(description = "전체 페이지 수", example = "5")
    int totalPages,

    @Schema(description = "전체 요소 수", example = "100")
    long totalElements,

    @Schema(description = "페이지 크기", example = "20")
    int size
){}
