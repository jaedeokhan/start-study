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
@Schema(description = "잔액 조회 응답")
public class BalanceResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "현재 잔액", example = "5000000")
    private Long balance;

    @Schema(description = "마지막 업데이트 시간", example = "2025-10-29T14:00:00")
    private LocalDateTime lastUpdatedAt;

    public static BalanceResponse from(User user) {
        return new BalanceResponse(
            user.getId(),
            user.getBalance(),
            user.getUpdatedAt()
        );
    }
}
