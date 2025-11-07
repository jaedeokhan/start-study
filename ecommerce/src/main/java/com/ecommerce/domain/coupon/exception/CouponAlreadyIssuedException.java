package com.ecommerce.domain.coupon.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CouponAlreadyIssuedException extends BaseException {
    public CouponAlreadyIssuedException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
