package com.ecommerce.application.usecase.product;

import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.product.ProductListResponse;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * US-PROD-001: 상품 목록 조회
 */
@Component
@RequiredArgsConstructor
public class GetProductsUseCase {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductListResponse execute(int page, int size) {
        // 1. 상품 목록 조회
        List<Product> products = productRepository.findAll(page, size);

        // 2. 페이지네이션 정보 계산
        int totalElements = productRepository.getTotalCount();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // 3. 응답 생성
        return ProductListResponse.from(products, page, size, totalElements, totalPages);
    }
}
