package com.ecommerce.presentation.dto.payment;

import com.ecommerce.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "잔액 충전 응답")
public class ChargeBalanceResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "이전 잔액", example = "5000000")
    private Long previousBalance;

    @Schema(description = "충전 금액", example = "1000000")
    private Long chargedAmount;

    @Schema(description = "현재 잔액", example = "6000000")
    private Long currentBalance;

    @Schema(description = "충전 시간", example = "2025-10-29T14:30:00")
    private LocalDateTime chargedAt;

    public static ChargeBalanceResponse from(User user, long previousBalance, long chargedAmount) {
        return new ChargeBalanceResponse(
            user.getId(),
            previousBalance,
            chargedAmount,
            user.getBalance(),
            user.getUpdatedAt()
        );
    }
}
