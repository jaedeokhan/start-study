package com.ecommerce.domain.product.exception;

import com.ecommerce.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 상품 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "재고가 부족합니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
