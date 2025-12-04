package com.ecommerce.application.usecase.product;

import com.ecommerce.domain.product.Product;
import com.ecommerce.infrastructure.redis.ProductRankingRepository;
import com.ecommerce.infrastructure.repository.OrderItemRepository;
import com.ecommerce.infrastructure.repository.OrderItemRepository.ProductSalesProjection;
import com.ecommerce.infrastructure.repository.ProductRepository;
import com.ecommerce.presentation.dto.product.PopularProductResponse;
import com.ecommerce.presentation.dto.product.ProductRankingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProductRepository productRepository;
    private final ProductRankingRepository productRankingRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "product:popular")
    public PopularProductResponse execute() {
        // 1. Redis에서 최근 3일간 Top 5 조회
        List<ProductRankingDto> topRankings =
                productRankingRepository.getTop5Last3Days();

        // 2. 상품 ID 및 판매량 맵 생성
        List<Long> topProductIds = topRankings.stream()
                .map(ProductRankingDto::productId)
                .collect(Collectors.toList());

        Map<Long, Integer> salesCountMap = topRankings.stream()
                .collect(Collectors.toMap(
                        ProductRankingDto::productId,
                        ProductRankingDto::totalQuantity
                ));

        // 3. 상품 정보 조회 (DB)
        List<Product> popularProducts = productRepository.findAllById(topProductIds);

        // 4. 응답 생성
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        return PopularProductResponse.from(popularProducts, salesCountMap, threeDaysAgo);
    }
}
