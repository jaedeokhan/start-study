package com.ecommerce.domain.cart.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class CartItemNotFoundException extends BaseException {
    public CartItemNotFoundException(CartErrorCode errorCode) {
        super(errorCode);
    }
}
