package com.ecommerce.presentation.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 응답")
public class ProductResponse {

    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @Schema(description = "상품명", example = "노트북")
    private String name;

    @Schema(description = "상품 설명", example = "고성능 노트북, 16GB RAM, 512GB SSD")
    private String description;

    @Schema(description = "가격", example = "1500000")
    private Long price;

    @Schema(description = "재고", example = "50")
    private Integer stock;

    @Schema(description = "생성 시간", example = "2025-10-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2025-10-29T14:00:00")
    private LocalDateTime updatedAt;
}
