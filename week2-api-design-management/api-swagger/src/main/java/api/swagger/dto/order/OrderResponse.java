package api.swagger.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주문 상세 응답")
public class OrderResponse {

    @Schema(description = "주문 ID", example = "12345")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "주문 상태", example = "COMPLETED")
    private String status;

    @Schema(description = "주문 상품 목록")
    private List<OrderItem> items;

    @Schema(description = "원래 금액", example = "3000000")
    private Long originalAmount;

    @Schema(description = "할인 금액", example = "100000")
    private Long discountAmount;

    @Schema(description = "최종 결제 금액", example = "2900000")
    private Long finalAmount;

    @Schema(description = "사용된 쿠폰 정보")
    private CouponUsed couponUsed;

    @Schema(description = "결제 정보")
    private PaymentInfo paymentInfo;

    @Schema(description = "주문 생성 시간", example = "2025-10-29T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "주문 수정 시간", example = "2025-10-29T14:30:00")
    private LocalDateTime updatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주문 상품 정보")
    public static class OrderItem {
        @Schema(description = "주문 상품 ID", example = "1")
        private Long orderItemId;

        @Schema(description = "상품 ID", example = "1")
        private Long productId;

        @Schema(description = "상품명", example = "노트북")
        private String productName;

        @Schema(description = "상품 단가", example = "1500000")
        private Long price;

        @Schema(description = "수량", example = "2")
        private Integer quantity;

        @Schema(description = "소계 (단가 x 수량)", example = "3000000")
        private Long subtotal;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용된 쿠폰 정보")
    public static class CouponUsed {
        @Schema(description = "쿠폰 ID", example = "5")
        private Long couponId;

        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        private String couponName;

        @Schema(description = "할인 금액", example = "100000")
        private Long discountAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결제 정보")
    public static class PaymentInfo {
        @Schema(description = "결제 ID", example = "678")
        private Long paymentId;

        @Schema(description = "결제 수단", example = "BALANCE")
        private String method;

        @Schema(description = "결제 금액", example = "2900000")
        private Long amount;

        @Schema(description = "결제 완료 시간", example = "2025-10-29T14:30:00")
        private LocalDateTime paidAt;
    }
}
