package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.order.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>  {

    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable
            pageable);

    Optional<Order> findById(Long id);

    default List<Order> findByUserId(Long userId, int page, int size) {
        return findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    List<Order> findByCreatedAtAfter(LocalDateTime dateTime);

    Order save(Order order);

}
