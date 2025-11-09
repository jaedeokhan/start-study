package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChargePointResponse {
    private Long userId;
    private long previousBalance;
    private long chargedAmount;
    private long currentBalance;
    private LocalDateTime chargedAt;

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
