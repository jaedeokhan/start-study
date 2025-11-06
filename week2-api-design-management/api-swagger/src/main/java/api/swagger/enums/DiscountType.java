package api.swagger.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "할인 유형")
public enum DiscountType {
    @Schema(description = "고정 금액 할인")
    AMOUNT,

    @Schema(description = "비율 할인")
    RATE
}
