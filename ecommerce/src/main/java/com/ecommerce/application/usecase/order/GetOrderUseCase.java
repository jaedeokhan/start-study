package com.ecommerce.application.usecase.order;

import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.domain.order.exception.OrderErrorCode;
import com.ecommerce.domain.order.exception.OrderNotFoundException;
import com.ecommerce.infrastructure.repository.OrderRepository;
import com.ecommerce.presentation.dto.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * US-ORD-002: 주문 상세 조회
 */
@Component
@RequiredArgsConstructor
public class GetOrderUseCase {
    private final OrderRepository orderRepository;

    public OrderResponse execute(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(OrderErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 아이템 조회
        List<OrderItem> orderItems = orderRepository.findOrderItemsByOrderId(orderId);

        // 3. 응답 생성
        return OrderResponse.from(order, orderItems);
    }
}
