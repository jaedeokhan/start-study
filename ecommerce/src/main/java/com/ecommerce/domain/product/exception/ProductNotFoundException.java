package com.ecommerce.domain.product.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class ProductNotFoundException extends BaseException {
    public ProductNotFoundException(ProductErrorCode errorCode) {
        super(errorCode);
    }
}
