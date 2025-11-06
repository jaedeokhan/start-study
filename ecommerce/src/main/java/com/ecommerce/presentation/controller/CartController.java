package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.cart.AddCartItemUseCase;
import com.ecommerce.application.usecase.cart.GetCartItemsUseCase;
import com.ecommerce.application.usecase.cart.RemoveCartItemUseCase;
import com.ecommerce.application.usecase.cart.UpdateCartItemQuantityUseCase;
import com.ecommerce.presentation.api.CartApi;
import com.ecommerce.presentation.dto.cart.AddCartItemRequest;
import com.ecommerce.presentation.dto.cart.AddCartItemResponse;
import com.ecommerce.presentation.dto.cart.CartResponse;
import com.ecommerce.presentation.dto.cart.UpdateCartItemRequest;
import com.ecommerce.presentation.dto.cart.UpdateCartItemResponse;
import com.ecommerce.presentation.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장바구니 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequiredArgsConstructor
public class CartController implements CartApi {
    // ✅ UseCase 주입
    private final GetCartItemsUseCase getCartItemsUseCase;
    private final AddCartItemUseCase addCartItemUseCase;
    private final UpdateCartItemQuantityUseCase updateCartItemQuantityUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;

    @Override
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Long userId) {
        CartResponse response = getCartItemsUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AddCartItemResponse>> addCartItem(AddCartItemRequest request) {
        AddCartItemResponse response = addCartItemUseCase.execute(
            request.getUserId(),
            request.getProductId(),
            request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<UpdateCartItemResponse>> updateCartItem(
            Long cartItemId,
            UpdateCartItemRequest request
    ) {
        UpdateCartItemResponse response = updateCartItemQuantityUseCase.execute(
            cartItemId,
            request.getQuantity()
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<Void> deleteCartItem(Long cartItemId) {
        removeCartItemUseCase.execute(cartItemId);
        return ResponseEntity.noContent().build();
    }
}
