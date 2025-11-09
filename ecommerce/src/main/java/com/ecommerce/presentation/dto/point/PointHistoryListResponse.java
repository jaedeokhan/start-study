package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.point.PointHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class PointHistoryListResponse {
    private Long userId;
    private List<PointHistoryResponse> histories;
    private int totalCount;

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
