package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.domain.coupon.exception.CouponAlreadyUsedException;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 Entity
 * - 쿠폰 사용 관리 비즈니스 로직 포함
 */
@Getter
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponEventId;
    private boolean isUsed;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;

    // 생성자
    public UserCoupon(Long id, Long userId, Long couponEventId,
                      LocalDateTime validFrom, LocalDateTime validUntil) {
        this.id = id;
        this.userId = userId;
        this.couponEventId = couponEventId;
        this.isUsed = false;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.issuedAt = LocalDateTime.now();
    }

    // ========== 검증 및 비즈니스 로직 ==========

    /**
     * 쿠폰 사용 가능 여부 검증
     * @throws CouponAlreadyUsedException 이미 사용된 쿠폰
     * @throws CouponExpiredException 유효기간 만료
     */
    public void validateUsable() {
        if (this.isUsed) {
            throw new CouponAlreadyUsedException("이미 사용된 쿠폰입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(validFrom) || now.isAfter(validUntil)) {
            throw new CouponExpiredException(
                String.format("쿠폰 만료: 유효기간 %s ~ %s", validFrom, validUntil)
            );
        }
    }

    /**
     * 쿠폰 사용 처리
     * @throws CouponAlreadyUsedException 이미 사용됨
     * @throws CouponExpiredException 유효기간 만료
     */
    public void use() {
        validateUsable();
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validUntil);
    }

    /**
     * 사용 가능 여부 확인 (예외 없이)
     */
    public boolean canUse() {
        if (this.isUsed) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    /**
     * 쿠폰 상태 조회
     * CouponEvent 정보와 현재 시간을 기반으로 상태 판단
     */
    public CouponStatus getStatus(CouponEvent couponEvent, LocalDateTime now) {
        if (this.isUsed) {
            return CouponStatus.USED;
        }
        if (now.isAfter(this.validUntil)) {
            return CouponStatus.EXPIRED;
        }
        return CouponStatus.AVAILABLE;
    }
}
