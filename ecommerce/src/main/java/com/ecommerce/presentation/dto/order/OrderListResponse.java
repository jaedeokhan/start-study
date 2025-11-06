package com.ecommerce.presentation.dto.order;

import com.ecommerce.presentation.dto.common.PaginationInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주문 목록 응답")
public class OrderListResponse {

    @Schema(description = "주문 목록")
    private List<OrderSummary> orders;

    @Schema(description = "페이지네이션 정보")
    private PaginationInfo pagination;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주문 요약 정보")
    public static class OrderSummary {
        @Schema(description = "주문 ID", example = "12345")
        private Long orderId;

        @Schema(description = "주문 상태", example = "COMPLETED")
        private String status;

        @Schema(description = "원래 금액", example = "3000000")
        private Long originalAmount;

        @Schema(description = "할인 금액", example = "100000")
        private Long discountAmount;

        @Schema(description = "최종 결제 금액", example = "2900000")
        private Long finalAmount;

        @Schema(description = "주문 상품 개수", example = "2")
        private Integer itemCount;

        @Schema(description = "주문 생성 시간", example = "2025-10-29T14:30:00")
        private LocalDateTime createdAt;
    }
}
