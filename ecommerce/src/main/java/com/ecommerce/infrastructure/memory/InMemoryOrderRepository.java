package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.infrastructure.repository.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Order InMemory Repository 구현체
 * - ConcurrentHashMap 기반 인메모리 저장소
 * - Order와 OrderItem을 별도로 관리
 */
@Repository
@Profile("memory")
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<Long, Order> orderStore = new ConcurrentHashMap<>();
    private final Map<Long, List<OrderItem>> orderItemStore = new ConcurrentHashMap<>();
    private final AtomicLong orderIdGenerator = new AtomicLong(1);
    private final AtomicLong orderItemIdGenerator = new AtomicLong(1);

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderStore.get(id));
    }

    @Override
    public List<Order> findByUserId(Long userId, int page, int size) {
        return orderStore.values().stream()
            .filter(order -> order.getUserId().equals(userId))
            .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
            .skip((long) page * size)
            .limit(size)
            .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByCreatedAtAfter(LocalDateTime dateTime) {
        return orderStore.values().stream()
            .filter(order -> order.getCreatedAt().isAfter(dateTime))
            .collect(Collectors.toList());
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            Long newId = orderIdGenerator.getAndIncrement();
            Order newOrder = new Order(
                newId,
                order.getUserId(),
                order.getOriginalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getCouponId()
            );
            orderStore.put(newId, newOrder);
            return newOrder;
        } else {
            orderStore.put(order.getId(), order);
            return order;
        }
    }

    @Override
    public OrderItem saveOrderItem(OrderItem orderItem) {
        Long newId = orderItemIdGenerator.getAndIncrement();
        OrderItem newItem = new OrderItem(
            newId,
            orderItem.getOrderId(),
            orderItem.getProductId(),
            orderItem.getProductName(),
            orderItem.getQuantity(),
            orderItem.getPrice()
        );

        // OrderItem을 Order별로 그룹화하여 저장
        orderItemStore.computeIfAbsent(orderItem.getOrderId(), k -> new ArrayList<>()).add(newItem);

        return newItem;
    }

    @Override
    public List<OrderItem> findOrderItemsByOrderId(Long orderId) {
        return orderItemStore.getOrDefault(orderId, new ArrayList<>());
    }
}
