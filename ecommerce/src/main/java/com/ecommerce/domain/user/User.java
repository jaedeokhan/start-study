package com.ecommerce.domain.user;

import com.ecommerce.domain.common.exception.BaseTimeEntity;
import com.ecommerce.domain.point.exception.InsufficientPointException;
import com.ecommerce.domain.point.exception.PointErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "point_balance", nullable = false)
    private long pointBalance;

    @Version
    @Column(name = "version")
    private Long version;

    public User(Long id, String name, long pointBalance) {
        validatePointBalance(pointBalance);

        this.id = id;
        this.name = name;
        this.pointBalance = pointBalance;
    }

    public static User create(String name, long pointBalance) {
        return new User(null, name, pointBalance);
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

    public void chargePoint(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.pointBalance += amount;
    }

    public void usePoint(long amount) {
        if (this.pointBalance < amount) {
            throw new InsufficientPointException(PointErrorCode.INSUFFICIENT_POINT);
        }
        this.pointBalance -= amount;
    }
}