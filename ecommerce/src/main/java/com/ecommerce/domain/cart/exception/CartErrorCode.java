package com.ecommerce.domain.cart.exception;

import com.ecommerce.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 장바구니 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {

    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", "장바구니 아이템을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    EMPTY_CART("EMPTY_CART", "장바구니가 비어있습니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
