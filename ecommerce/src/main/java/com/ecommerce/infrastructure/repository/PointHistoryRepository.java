package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.point.PointHistory;

import java.util.List;

/**
 * PointHistory Repository Interface
 */
public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findByUserId(Long userId);
    List<PointHistory> findByUserIdWithPagination(Long userId, int offset, int limit);
}
