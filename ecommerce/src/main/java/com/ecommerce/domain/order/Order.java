package com.ecommerce.domain.order;

import com.ecommerce.domain.order.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주문 Entity
 * - 주문 상태 관리 비즈니스 로직 포함
 */
@Getter
public class Order {
    private Long id;
    private Long userId;
    private OrderStatus status;
    private long originalAmount;    // 원래 금액
    private long discountAmount;    // 할인 금액
    private long finalAmount;       // 최종 결제 금액
    private Long couponId;          // 사용한 쿠폰 ID (nullable)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자
    public Order(Long id, Long userId, long originalAmount,
                 long discountAmount, long finalAmount, Long couponId) {
        this.id = id;
        this.userId = userId;
        this.status = OrderStatus.PENDING;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.couponId = couponId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 주문 완료 처리
     */
    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소 처리
     */
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 완료 여부 확인
     */
    public boolean isCompleted() {
        return this.status == OrderStatus.COMPLETED;
    }

    /**
     * 주문 취소 여부 확인
     */
    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }
}
