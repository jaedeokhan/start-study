package com.ecommerce.domain.product.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class InsufficientStockException extends BaseException {
    public InsufficientStockException(ProductErrorCode errorCode) {
        super(errorCode);
    }
}
