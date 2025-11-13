package com.ecommerce.infrastructure.jpa.impl;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.infrastructure.jpa.PointHistoryJpaRepository;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointHistoryJpaRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return pointHistoryJpaRepository.save(pointHistory);
    }

    @Override
    public List<PointHistory> findByUserId(Long userId) {
        return pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<PointHistory> findByUserIdWithPagination(Long userId, int offset, int limit) {
        int page = offset / limit;
        return pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, limit)
        );
    }
}
