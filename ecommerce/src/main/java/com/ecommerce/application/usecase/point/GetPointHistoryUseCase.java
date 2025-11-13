package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import com.ecommerce.presentation.dto.point.PointHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * US-PAY-008: 포인트 이력 조회
 */
@Component
@RequiredArgsConstructor
public class GetPointHistoryUseCase {
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public PointHistoryListResponse execute(Long userId) {
        List<PointHistory> histories = pointHistoryRepository.findByUserId(userId);
        return PointHistoryListResponse.from(userId, histories);
    }
}
