package com.ecommerce.domain.user;

import com.ecommerce.domain.payment.exception.InsufficientBalanceException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 Entity
 * - 잔액 관리 비즈니스 로직 포함
 */
@Getter
public class User {
    private Long id;
    private String name;
    private long balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자
    public User(Long id, String name, long balance) {
        validateBalance(balance);

        this.id = id;
        this.name = name;
        this.balance = balance;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 검증 로직 (Entity 내부) ==========

    /**
     * 잔액 검증
     */
    private void validateBalance(long balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("잔액은 0 이상이어야 합니다.");
        }
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 잔액 확인 (차감하지 않음)
     * @param amount 필요한 금액
     * @return 잔액 충분 여부
     */
    public boolean hasBalance(long amount) {
        return this.balance >= amount;
    }

    /**
     * 잔액 충전
     * @param amount 충전할 금액
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
     */
    public void charge(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔액 차감
     * @param amount 차감할 금액
     * @throws InsufficientBalanceException 잔액 부족 시
     */
    public void deduct(long amount) {
        if (this.balance < amount) {
            throw new InsufficientBalanceException(
                String.format("잔액 부족: 필요 금액 %d원, 현재 잔액 %d원", amount, balance)
            );
        }
        this.balance -= amount;
        this.updatedAt = LocalDateTime.now();
    }
}
