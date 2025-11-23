package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.point.PointHistory;

import java.util.List;
import java.util.stream.Collectors;

public record PointHistoryListResponse (
    Long userId,
    List<PointHistoryResponse> histories,
    int totalCount
) {
    public static PointHistoryListResponse from(Long userId, List<PointHistory> histories) {
        List<PointHistoryResponse> historyResponses = histories.stream()
            .map(PointHistoryResponse::from)
            .collect(Collectors.toList());

        return new PointHistoryListResponse(
            userId,
            historyResponses,
            histories.size()
        );
    }
}
