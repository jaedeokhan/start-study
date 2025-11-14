package com.ecommerce.application.usecase.order;

import com.ecommerce.domain.order.Order;
import com.ecommerce.infrastructure.repository.OrderRepository;
import com.ecommerce.presentation.dto.order.OrderListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * US-ORD-003: 주문 목록 조회
 */
@Component
@RequiredArgsConstructor
public class GetOrdersUseCase {
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderListResponse execute(Long userId, int page, int size) {
        // 1. 주문 목록 조회
        List<Order> orders = orderRepository.findByUserId(userId, page, size);

        // 2. 페이지네이션 정보 계산 (simplified - use orders size)
        int totalElements = orders.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // 3. 응답 생성
        return OrderListResponse.from(orders, page, size, totalElements, totalPages);
    }
}
