package com.ecommerce.domain.coupon.exception;

public class CouponEventNotFoundException extends RuntimeException {
    public CouponEventNotFoundException(String message) {
        super(message);
    }
}
