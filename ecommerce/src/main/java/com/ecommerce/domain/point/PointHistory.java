package com.ecommerce.domain.point;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 포인트 이력 Entity
 * - 포인트 충전/사용/환불 이력 추적
 */
@Getter
public class PointHistory {
    private Long id;
    private Long userId;
    private long pointAmount;           // 포인트 변경량 (충전: 양수, 사용: 음수)
    private TransactionType transactionType;
    private long balanceAfter;          // 거래 후 잔액
    private Long orderId;               // 주문 ID (USE, REFUND 타입일 때)
    private String description;
    private LocalDateTime createdAt;

    // 생성자 - 포인트 충전용
    public PointHistory(Long id, Long userId, long pointAmount, TransactionType transactionType,
                        long balanceAfter, String description) {
        this(id, userId, pointAmount, transactionType, balanceAfter, null, description);
    }

    // 생성자 - 포인트 사용/환불용 (주문 ID 포함)
    public PointHistory(Long id, Long userId, long pointAmount, TransactionType transactionType,
                        long balanceAfter, Long orderId, String description) {
        validatePointAmount(pointAmount, transactionType);

        this.id = id;
        this.userId = userId;
        this.pointAmount = pointAmount;
        this.transactionType = transactionType;
        this.balanceAfter = balanceAfter;
        this.orderId = orderId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 포인트 변경량 검증
     */
    private void validatePointAmount(long pointAmount, TransactionType type) {
        if (type == TransactionType.CHARGE && pointAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (type == TransactionType.USE && pointAmount >= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 작아야 합니다.");
        }
        if (type == TransactionType.REFUND && pointAmount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
    }
}
