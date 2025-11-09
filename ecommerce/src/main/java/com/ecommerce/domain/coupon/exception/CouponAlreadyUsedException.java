package com.ecommerce.domain.coupon.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CouponAlreadyUsedException extends BaseException {
    public CouponAlreadyUsedException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
