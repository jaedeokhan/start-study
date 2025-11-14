package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_amount")
    private long discountAmount;      // AMOUNT 타입일 때 사용

    @Column(name = "discount_rate")
    private int discountRate;         // RATE 타입일 때 사용 (%)

    @Column(name = "max_discount_amount")
    private int maxDiscountAmount;    // RATE 타입의 최대 할인액

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;        // 총 발급 가능 수량

    @Column(name = "issued_quantity", nullable = false)
    private int issuedQuantity;       // 현재까지 발급된 수량

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;  // 이벤트 시작일

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;    // 이벤트 종료일

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // AMOUNT 타입
    public CouponEvent(Long id, String name, DiscountType discountType,
                       long discountAmount, int totalQuantity,
                       LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.name = name;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
    }

    // RATE 타입
    public CouponEvent(Long id, String name, DiscountType discountType,
                       int discountRate, int maxDiscountAmount, int totalQuantity,
                       LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.name = name;
        this.discountType = discountType;
        this.discountRate = discountRate;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 발급 가능 여부 확인
     */
    public boolean canIssue() {
        return this.issuedQuantity < this.totalQuantity;
    }

    public void issue() {
        if (!canIssue()) {
            throw new CouponSoldOutException(CouponErrorCode.COUPON_SOLD_OUT);
        }
        this.issuedQuantity++;
    }

    /**
     * 남은 수량 조회
     */
    public int getRemainingQuantity() {
        return this.totalQuantity - this.issuedQuantity;
    }

    /**
     * 이벤트 진행 중 여부 확인
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * 쿠폰 발급 가능 여부 확인 (기간 + 수량)
     */
    public boolean isAvailable(LocalDateTime now) {
        boolean inPeriod = !now.isBefore(startDate) && !now.isAfter(endDate);
        return inPeriod && canIssue();
    }
}
