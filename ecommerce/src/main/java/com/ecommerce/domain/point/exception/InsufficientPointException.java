package com.ecommerce.domain.point.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class InsufficientPointException extends BaseException {
    public InsufficientPointException(PointErrorCode errorCode) {
        super(errorCode);
    }
}
