package com.ecommerce.domain.payment.exception;

public class InsufficientPointException extends RuntimeException {
    public InsufficientPointException(String message) {
        super(message);
    }
}
