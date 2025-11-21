package com.ecommerce.presentation.dto.product;

import com.ecommerce.domain.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "상품 응답")
public record ProductResponse (

    @Schema(description = "상품 ID", example = "1")
    Long id,

    @Schema(description = "상품명", example = "노트북")
    String name,

    @Schema(description = "상품 설명", example = "고성능 노트북, 16GB RAM, 512GB SSD")
    String description,

    @Schema(description = "가격", example = "1500000")
    Long price,

    @Schema(description = "재고", example = "50")
    Integer stock,

    @Schema(description = "생성 시간", example = "2025-10-01T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 시간", example = "2025-10-29T14:00:00")
    LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
