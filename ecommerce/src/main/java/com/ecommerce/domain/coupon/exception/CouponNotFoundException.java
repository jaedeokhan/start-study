package com.ecommerce.domain.coupon.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CouponNotFoundException extends BaseException {
    public CouponNotFoundException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
