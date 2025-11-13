package com.ecommerce.application.usecase.product;

import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.infrastructure.repository.OrderItemRepository;
import com.ecommerce.presentation.dto.product.PopularProductResponse;
import com.ecommerce.infrastructure.repository.OrderRepository;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * US-PROD-003: 인기 상품 조회 (최근 3일 판매량 Top 5)
 */
@Component
@RequiredArgsConstructor
public class GetPopularProductsUseCase {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public PopularProductResponse execute() {
        // 1. 기준 시점 계산
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // 2. 최근 3일간 주문 조회
        List<Order> recentOrders = orderRepository.findByCreatedAtAfter(threeDaysAgo);

        // 3. 상품별 판매량 집계
        Map<Long, Integer> salesCountMap = new HashMap<>();
        for (Order order : recentOrders) {
            List<OrderItem> items = orderItemRepository.findOrderItemsByOrderId(order.getId());
            for (OrderItem item : items) {
                salesCountMap.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        // 4. 판매량 기준 Top 5 추출
        List<Long> topProductIds = salesCountMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 5. 상품 정보 조회
        List<Product> popularProducts = productRepository.findAllById(topProductIds);

        // 6. 응답 생성
        return PopularProductResponse.from(popularProducts, salesCountMap, threeDaysAgo);
    }
}
