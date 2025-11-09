package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Order Repository Interface
 */
public interface OrderRepository {
    Optional<Order> findById(Long id);
    List<Order> findByUserId(Long userId, int page, int size);
    List<Order> findByCreatedAtAfter(LocalDateTime dateTime);
    Order save(Order order);
    OrderItem saveOrderItem(OrderItem orderItem);
    List<OrderItem> findOrderItemsByOrderId(Long orderId);
}
