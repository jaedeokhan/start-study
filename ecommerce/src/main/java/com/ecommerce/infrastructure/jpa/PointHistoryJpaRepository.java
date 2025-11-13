package com.ecommerce.infrastructure.jpa;

import com.ecommerce.domain.point.PointHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
