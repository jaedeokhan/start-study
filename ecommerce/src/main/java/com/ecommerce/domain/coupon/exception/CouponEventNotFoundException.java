package com.ecommerce.domain.coupon.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CouponEventNotFoundException extends BaseException {
    public CouponEventNotFoundException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
