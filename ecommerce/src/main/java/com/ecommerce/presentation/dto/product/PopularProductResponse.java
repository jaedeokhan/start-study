package com.ecommerce.presentation.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인기 상품 응답")
public class PopularProductResponse {

    @Schema(description = "인기 상품 목록")
    private List<PopularProduct> products;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인기 상품 정보")
    public static class PopularProduct {
        @Schema(description = "상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "노트북")
        private String name;

        @Schema(description = "가격", example = "1500000")
        private Long price;

        @Schema(description = "재고", example = "50")
        private Integer stock;

        @Schema(description = "판매 수량", example = "150")
        private Integer salesCount;

        @Schema(description = "판매 집계 기간")
        private SalesPeriod salesPeriod;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판매 집계 기간")
    public static class SalesPeriod {
        @Schema(description = "시작일", example = "2025-10-26T00:00:00")
        private LocalDateTime startDate;

        @Schema(description = "종료일", example = "2025-10-29T00:00:00")
        private LocalDateTime endDate;
    }
}
