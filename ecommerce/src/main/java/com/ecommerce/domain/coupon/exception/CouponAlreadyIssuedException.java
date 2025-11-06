package com.ecommerce.domain.coupon.exception;

public class CouponAlreadyIssuedException extends RuntimeException {
    public CouponAlreadyIssuedException(String message) {
        super(message);
    }
}
