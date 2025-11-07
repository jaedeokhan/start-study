package com.ecommerce.domain.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 인터페이스
 * - 모든 도메인별 ErrorCode enum이 구현해야 하는 인터페이스
 */
public interface ErrorCode {

    /**
     * 에러 코드 반환
     * @return 에러 코드 (예: "PRODUCT_NOT_FOUND")
     */
    String getCode();

    /**
     * 에러 메시지 반환
     * @return 에러 메시지 (예: "상품을 찾을 수 없습니다")
     */
    String getMessage();

    /**
     * HTTP 상태 코드 반환
     * @return HTTP 상태 코드 (예: HttpStatus.NOT_FOUND)
     */
    HttpStatus getHttpStatus();
}
