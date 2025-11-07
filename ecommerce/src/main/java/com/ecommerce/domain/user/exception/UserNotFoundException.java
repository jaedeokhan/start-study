package com.ecommerce.domain.user.exception;

import com.ecommerce.domain.common.exception.BaseException;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(UserErrorCode errorCode) {
        super(errorCode);
    }

    public UserNotFoundException(UserErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
