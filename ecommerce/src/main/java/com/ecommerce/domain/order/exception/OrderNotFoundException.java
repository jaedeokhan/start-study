package com.ecommerce.domain.order.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class OrderNotFoundException extends BaseException {
    public OrderNotFoundException(OrderErrorCode errorCode) {
        super(errorCode);
    }
}
