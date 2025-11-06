package com.ecommerce.presentation.dto.point;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChargePointRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotNull(message = "충전 금액은 필수입니다")
    @Min(value = 1, message = "충전 금액은 1 이상이어야 합니다")
    private Long amount;
}
