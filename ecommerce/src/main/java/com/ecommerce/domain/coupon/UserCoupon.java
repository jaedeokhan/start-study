package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.CouponStatus;
import com.ecommerce.domain.coupon.exception.CouponAlreadyUsedException;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
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
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;

    // 생성자
    public UserCoupon(Long id, Long userId, Long couponEventId,
                      LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.userId = userId;
        this.couponEventId = couponEventId;
        this.isUsed = false;
        this.startDate = startDate;
        this.endDate = endDate;
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
            throw new CouponAlreadyUsedException(CouponErrorCode.COUPON_ALREADY_USED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate) || now.isAfter(endDate)) {
            throw new CouponExpiredException(CouponErrorCode.COUPON_EXPIRED);
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
        return LocalDateTime.now().isAfter(endDate);
    }

    /**
     * 사용 가능 여부 확인 (예외 없이)
     */
    public boolean canUse() {
        if (this.isUsed) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * 쿠폰 상태 조회
     * CouponEvent 정보와 현재 시간을 기반으로 상태 판단
     */
    public CouponStatus getStatus(CouponEvent couponEvent, LocalDateTime now) {
        if (this.isUsed) {
            return CouponStatus.USED;
        }
        if (now.isAfter(this.endDate)) {
            return CouponStatus.EXPIRED;
        }
        return CouponStatus.AVAILABLE;
    }
}
