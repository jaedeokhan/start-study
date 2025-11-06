package com.ecommerce.application.usecase.product;

import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.product.ProductResponse;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-PROD-002: 상품 상세 조회
 */
@Component
@RequiredArgsConstructor
public class GetProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponse execute(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다: " + productId));

        // 2. 응답 생성
        return ProductResponse.from(product);
    }
}
