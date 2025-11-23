package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.user.User;

import java.time.LocalDateTime;

public record ChargePointResponse (
    Long userId,
    long previousBalance,
    long chargedAmount,
    long currentBalance,
    LocalDateTime chargedAt
) {
    public static ChargePointResponse from(User user, long previousBalance, long chargedAmount) {
        return new ChargePointResponse(
            user.getId(),
            previousBalance,
            chargedAmount,
            user.getPointBalance(),
            user.getUpdatedAt()
        );
    }
}
