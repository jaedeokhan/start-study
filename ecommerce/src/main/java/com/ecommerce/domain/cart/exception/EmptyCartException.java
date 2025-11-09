package com.ecommerce.domain.cart.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class EmptyCartException extends BaseException {
    public EmptyCartException(CartErrorCode errorCode) {
        super(errorCode);
    }
}
