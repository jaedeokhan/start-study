package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import com.ecommerce.presentation.dto.point.PointHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * US-PAY-008: 포인트 이력 조회
 */
@Component
@RequiredArgsConstructor
public class GetPointHistoryUseCase {
    private final PointHistoryRepository pointHistoryRepository;

    public PointHistoryListResponse execute(Long userId) {
        List<PointHistory> histories = pointHistoryRepository.findByUserId(userId);
        return PointHistoryListResponse.from(userId, histories);
    }

    public PointHistoryListResponse executeWithPagination(Long userId, int page, int size) {
        int offset = page * size;
        List<PointHistory> histories = pointHistoryRepository.findByUserIdWithPagination(userId, offset, size);
        return PointHistoryListResponse.from(userId, histories);
    }
}
