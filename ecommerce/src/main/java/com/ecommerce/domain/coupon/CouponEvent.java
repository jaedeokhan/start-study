package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 쿠폰 이벤트 Entity
 * - 쿠폰 발급 관리 비즈니스 로직 포함
 */
@Getter
public class CouponEvent {
    private Long id;
    private String name;
    private DiscountType discountType;
    private long discountAmount;      // AMOUNT 타입일 때 사용
    private int discountRate;         // RATE 타입일 때 사용 (%)
    private int maxDiscountAmount;    // RATE 타입의 최대 할인액
    private int totalQuantity;        // 총 발급 가능 수량
    private int issuedQuantity;       // 현재까지 발급된 수량
    private LocalDateTime startDate;  // 이벤트 시작일
    private LocalDateTime endDate;    // 이벤트 종료일
    private LocalDateTime createdAt;

    // 생성자 - AMOUNT 타입
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

    // 생성자 - RATE 타입
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

    /**
     * 쿠폰 발급 (수량 증가)
     * @throws CouponSoldOutException 쿠폰 소진 시
     */
    public void issue() {
        if (!canIssue()) {
            throw new CouponSoldOutException(
                String.format("쿠폰 소진: '%s' (총 수량: %d)", name, totalQuantity)
            );
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
