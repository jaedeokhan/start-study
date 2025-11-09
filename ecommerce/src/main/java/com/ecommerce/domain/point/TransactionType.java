package com.ecommerce.domain.point;

/**
 * 포인트 거래 유형
 */
public enum TransactionType {
    CHARGE,  // 포인트 충전
    USE,     // 포인트 사용 (주문 결제)
    REFUND   // 포인트 환불 (주문 취소)
}
