package com.ecommerce.domain.user;

import com.ecommerce.domain.payment.exception.InsufficientPointException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 Entity
 * - 포인트 관리 비즈니스 로직 포함
 */
@Getter
public class User {
    private Long id;
    private String name;
    private long pointBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자
    public User(Long id, String name, long pointBalance) {
        validatePointBalance(pointBalance);

        this.id = id;
        this.name = name;
        this.pointBalance = pointBalance;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 검증 로직 (Entity 내부) ==========

    /**
     * 포인트 잔액 검증
     */
    private void validatePointBalance(long pointBalance) {
        if (pointBalance < 0) {
            throw new IllegalArgumentException("포인트 잔액은 0 이상이어야 합니다.");
        }
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 포인트 잔액 확인 (차감하지 않음)
     * @param amount 필요한 금액
     * @return 포인트 충분 여부
     */
    public boolean hasPoint(long amount) {
        return this.pointBalance >= amount;
    }

    /**
     * 포인트 충전
     * @param amount 충전할 금액
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
     */
    public void chargePoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.pointBalance += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트 사용
     * @param amount 사용할 금액
     * @throws InsufficientPointException 포인트 부족 시
     */
    public void usePoint(long amount) {
        if (this.pointBalance < amount) {
            throw new InsufficientPointException(
                String.format("포인트 부족: 필요 금액 %d원, 현재 포인트 %d원", amount, pointBalance)
            );
        }
        this.pointBalance -= amount;
        this.updatedAt = LocalDateTime.now();
    }
}
