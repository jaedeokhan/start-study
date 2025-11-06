package com.ecommerce.domain.coupon.exception;

public class CouponSoldOutException extends RuntimeException {
    public CouponSoldOutException(String message) {
        super(message);
    }
}
