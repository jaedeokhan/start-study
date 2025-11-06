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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController implements CartApi {
    // ✅ UseCase 주입
    private final GetCartItemsUseCase getCartItemsUseCase;
    private final AddCartItemUseCase addCartItemUseCase;
    private final UpdateCartItemQuantityUseCase updateCartItemQuantityUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@RequestParam Long userId) {
        CartResponse response = getCartItemsUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/items")
    @Override
    public ResponseEntity<ApiResponse<AddCartItemResponse>> addCartItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {
        AddCartItemResponse response = addCartItemUseCase.execute(
            request.getUserId(),
            request.getProductId(),
            request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PatchMapping("/items/{cartItemId}")
    @Override
    public ResponseEntity<ApiResponse<UpdateCartItemResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UpdateCartItemResponse response = updateCartItemQuantityUseCase.execute(
            cartItemId,
            request.getQuantity()
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Override
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId) {
        removeCartItemUseCase.execute(cartItemId);
        return ResponseEntity.noContent().build();
    }
}
