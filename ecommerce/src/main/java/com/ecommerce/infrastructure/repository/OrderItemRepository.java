package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findOrderItemsByOrderId(Long orderId);

    @Query("""
        SELECT oi.productId as productId, SUM(oi.quantity) as totalQuantity
        FROM OrderItem oi
        JOIN Order o ON oi.orderId = o.id
        WHERE o.createdAt >= :startDate
        GROUP BY oi.productId
        ORDER BY totalQuantity DESC
        LIMIT 5
        """)
    List<ProductSalesProjection> findTopProductsByRecentSales(@Param("startDate") LocalDateTime startDate);

    interface ProductSalesProjection {
        Long getProductId();
        Integer getTotalQuantity();
    }
}