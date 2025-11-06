package com.ecommerce.presentation.exception;

import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.domain.cart.exception.CartItemNotFoundException;
import com.ecommerce.domain.cart.exception.EmptyCartException;
import com.ecommerce.domain.order.exception.OrderNotFoundException;
import com.ecommerce.domain.point.exception.InsufficientPointException;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.domain.coupon.exception.CouponNotFoundException;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import com.ecommerce.domain.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import com.ecommerce.domain.coupon.exception.CouponAlreadyUsedException;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.presentation.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * - 모든 Controller에서 발생하는 예외를 통합 처리
 * - 일관된 에러 응답 형식 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 상품 관련 예외
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex) {
        log.error("ProductNotFoundException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("PRODUCT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException ex) {
        log.error("InsufficientStockException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INSUFFICIENT_STOCK", ex.getMessage()));
    }

    /**
     * 장바구니 관련 예외
     */
    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartItemNotFound(CartItemNotFoundException ex) {
        log.error("CartItemNotFoundException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("CART_ITEM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmptyCart(EmptyCartException ex) {
        log.error("EmptyCartException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("EMPTY_CART", ex.getMessage()));
    }

    /**
     * 주문 관련 예외
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
        log.error("OrderNotFoundException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("ORDER_NOT_FOUND", ex.getMessage()));
    }

    /**
     * 포인트 관련 예외
     */
    @ExceptionHandler(InsufficientPointException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientPoint(InsufficientPointException ex) {
        log.error("InsufficientPointException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INSUFFICIENT_POINT", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        log.error("UserNotFoundException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("USER_NOT_FOUND", ex.getMessage()));
    }

    /**
     * 쿠폰 관련 예외
     */
    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponNotFound(CouponNotFoundException ex) {
        log.error("CouponNotFoundException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("COUPON_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CouponSoldOutException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponSoldOut(CouponSoldOutException ex) {
        log.error("CouponSoldOutException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("COUPON_SOLD_OUT", ex.getMessage()));
    }

    @ExceptionHandler(CouponAlreadyIssuedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponAlreadyIssued(CouponAlreadyIssuedException ex) {
        log.error("CouponAlreadyIssuedException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("COUPON_ALREADY_ISSUED", ex.getMessage()));
    }

    @ExceptionHandler(CouponExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponExpired(CouponExpiredException ex) {
        log.error("CouponExpiredException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("COUPON_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(CouponAlreadyUsedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponAlreadyUsed(CouponAlreadyUsedException ex) {
        log.error("CouponAlreadyUsedException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("COUPON_ALREADY_USED", ex.getMessage()));
    }

    @ExceptionHandler(CouponEventNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponEventNotFound(CouponEventNotFoundException ex) {
        log.error("CouponEventNotFoundException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("COUPON_EVENT_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", "입력 값 검증 실패", errors));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_ARGUMENT", ex.getMessage()));
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
