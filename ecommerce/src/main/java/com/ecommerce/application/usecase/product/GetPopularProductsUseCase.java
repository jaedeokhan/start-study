package com.ecommerce.application.usecase.product;

import com.ecommerce.domain.product.Product;
import com.ecommerce.infrastructure.repository.OrderItemRepository;
import com.ecommerce.infrastructure.repository.OrderItemRepository.ProductSalesProjection;
import com.ecommerce.infrastructure.repository.ProductRepository;
import com.ecommerce.presentation.dto.product.PopularProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-PROD-003: 인기 상품 조회 (최근 3일 판매량 Top 5)
 */
@Component
@RequiredArgsConstructor
public class GetPopularProductsUseCase {
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "product:popular")
    public PopularProductResponse execute() {
        // 1. 기준 시점 계산
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // 2. 최근 3일간 판매량 Top 5 상품 조회 (단일 쿼리로 처리)
        List<ProductSalesProjection> topProducts = orderItemRepository.findTopProductsByRecentSales(threeDaysAgo);

        // 3. 상품 ID 및 판매량 맵 생성
        List<Long> topProductIds = topProducts.stream()
            .map(ProductSalesProjection::getProductId)
            .collect(Collectors.toList());

        Map<Long, Integer> salesCountMap = topProducts.stream()
            .collect(Collectors.toMap(
                ProductSalesProjection::getProductId,
                ProductSalesProjection::getTotalQuantity
            ));

        // 4. 상품 정보 조회
        List<Product> popularProducts = productRepository.findAllById(topProductIds);

        // 5. 응답 생성
        return PopularProductResponse.from(popularProducts, salesCountMap, threeDaysAgo);
    }
}
