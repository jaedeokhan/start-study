package com.ecommerce.presentation.dto.product;

import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.common.PaginationInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 목록 응답")
public class ProductListResponse {

    @Schema(description = "상품 목록")
    private List<ProductSummary> products;

    @Schema(description = "페이지네이션 정보")
    private PaginationInfo pagination;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 요약 정보")
    public static class ProductSummary {
        @Schema(description = "상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "노트북")
        private String name;

        @Schema(description = "상품 설명", example = "고성능 노트북")
        private String description;

        @Schema(description = "가격", example = "1500000")
        private Long price;

        @Schema(description = "재고", example = "50")
        private Integer stock;

        public static ProductSummary from(Product product) {
            return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock()
            );
        }
    }

    public static ProductListResponse from(List<Product> products, int page, int size, int totalElements, int totalPages) {
        List<ProductSummary> productSummaries = products.stream()
            .map(ProductSummary::from)
            .collect(Collectors.toList());

        PaginationInfo pagination = new PaginationInfo(page, totalPages, totalElements, size);

        return new ProductListResponse(productSummaries, pagination);
    }
}
