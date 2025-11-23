package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;

import java.time.LocalDateTime;

public record PointHistoryResponse (
    Long id,
    Long userId,
    long pointAmount,
    TransactionType transactionType,
    long balanceAfter,
    Long orderId,
    String description,
    LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
            history.getId(),
            history.getUserId(),
            history.getPointAmount(),
            history.getTransactionType(),
            history.getBalanceAfter(),
            history.getOrderId(),
            history.getDescription(),
            history.getCreatedAt()
        );
    }
}
