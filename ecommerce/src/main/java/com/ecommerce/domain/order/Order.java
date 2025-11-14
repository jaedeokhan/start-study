package com.ecommerce.domain.order;

import com.ecommerce.domain.common.exception.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_status_id_created_at", columnList = "status, created_at, id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "original_amount", nullable = false)
    private long originalAmount;    // 원래 금액

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;    // 할인 금액

    @Column(name = "final_amount", nullable = false)
    private long finalAmount;       // 최종 결제 금액

    @Column(name = "coupon_id")
    private Long couponId;          // 사용한 쿠폰 ID (nullable)

    public Order(Long id, Long userId, long originalAmount,
                 long discountAmount, long finalAmount, Long couponId) {
        this.id = id;
        this.userId = userId;
        this.status = OrderStatus.COMPLETED;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.couponId = couponId;
    }

    public static Order create(Long userId, long originalAmount,
                               long discountAmount, long finalAmount, Long couponId) {
        return new Order(null, userId, originalAmount, discountAmount, finalAmount, couponId);
    }

    // ========== 비즈니스 로직 ==========

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
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
