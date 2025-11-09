package com.ecommerce.domain.coupon.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CouponSoldOutException extends BaseException {
    public CouponSoldOutException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
