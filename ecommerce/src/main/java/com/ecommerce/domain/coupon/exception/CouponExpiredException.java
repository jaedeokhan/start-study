package com.ecommerce.domain.coupon.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CouponExpiredException extends BaseException {
    public CouponExpiredException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
