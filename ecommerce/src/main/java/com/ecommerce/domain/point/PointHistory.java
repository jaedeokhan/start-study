package com.ecommerce.domain.point;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "point_histories",
        indexes = {
                @Index(name = "idx_point_history_user_id", columnList = "user_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "point_amount", nullable = false)
    private long pointAmount;           // 포인트 변경량 (충전: 양수, 사용: 음수)

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;          // 거래 후 잔액

    @Column(name = "order_id")
    private Long orderId;               // 주문 ID (USE, REFUND 타입일 때)

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 포인트 충전용
    public PointHistory(Long id, Long userId, long pointAmount, TransactionType transactionType,
                        long balanceAfter, String description) {
        this(id, userId, pointAmount, transactionType, balanceAfter, null, description);
    }

    // 포인트 사용/환불용 (주문 ID 포함)
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
