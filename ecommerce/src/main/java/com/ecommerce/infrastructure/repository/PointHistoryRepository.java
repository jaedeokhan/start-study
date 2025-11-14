package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.point.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    PointHistory save(PointHistory pointHistory);

    default List<PointHistory> findByUserId(Long userId) {
        return findByUserIdOrderByCreatedAtDesc(userId);
    }
}
