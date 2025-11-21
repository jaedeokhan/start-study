package com.ecommerce.presentation.dto.order;

import com.ecommerce.domain.order.Order;
import com.ecommerce.presentation.dto.common.PaginationInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "주문 목록 응답")
public record OrderListResponse (

    @Schema(description = "주문 목록")
    List<OrderSummary> orders,

    @Schema(description = "페이지네이션 정보")
    PaginationInfo pagination
) {
    @Schema(description = "주문 요약 정보")
    public record OrderSummary (
        @Schema(description = "주문 ID", example = "12345")
        Long orderId,

        @Schema(description = "주문 상태", example = "COMPLETED")
        String status,

        @Schema(description = "원래 금액", example = "3000000")
        Long originalAmount,

        @Schema(description = "할인 금액", example = "100000")
        Long discountAmount,

        @Schema(description = "최종 결제 금액", example = "2900000")
        Long finalAmount,

        @Schema(description = "주문 상품 개수", example = "2")
        Integer itemCount,

        @Schema(description = "주문 생성 시간", example = "2025-10-29T14:30:00")
        LocalDateTime createdAt
) {}
    public static OrderListResponse from(List<Order> orders, int page, int size, int totalElements, int totalPages) {
        List<OrderSummary> orderSummaries = orders.stream()
            .map(order -> new OrderSummary(
                order.getId(),
                order.getStatus().name(),
                order.getOriginalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                0, // itemCount - simplified, would need OrderItem count query
                order.getCreatedAt()
            ))
            .collect(Collectors.toList());

        PaginationInfo pagination = new PaginationInfo(page, totalPages, totalElements, size);

        return new OrderListResponse(orderSummaries, pagination);
    }
}