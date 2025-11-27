package com.ecommerce.application.usecase.product;

import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.product.ProductResponse;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-PROD-002: 상품 상세 조회
 */
@Component
@RequiredArgsConstructor
public class GetProductUseCase {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "product:detail", key = "#productId")
    public ProductResponse execute(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findByIdOrThrow(productId);

        // 2. 응답 생성
        return ProductResponse.from(product);
    }
}
