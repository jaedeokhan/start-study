package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.exception.CouponAlreadyUsedException;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_coupons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_coupon",
                        columnNames = {"user_id", "coupon_event_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_event_id", nullable = false)
    private Long couponEventId;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

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
