package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PointHistoryResponse {
    private Long id;
    private Long userId;
    private long pointAmount;
    private TransactionType transactionType;
    private long balanceAfter;
    private Long orderId;
    private String description;
    private LocalDateTime createdAt;

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
