package com.ecommerce.presentation.dto.product;

public record ProductRankingDto(
        Long productId,
        Integer totalQuantity,
        Long rank
) {}