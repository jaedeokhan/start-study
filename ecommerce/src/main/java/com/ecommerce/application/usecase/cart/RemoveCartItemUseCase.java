package com.ecommerce.application.usecase.cart;

import com.ecommerce.domain.cart.exception.CartErrorCode;
import com.ecommerce.domain.cart.exception.CartItemNotFoundException;
import com.ecommerce.infrastructure.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-CART-004: 장바구니 상품 삭제
 */
@Component
@RequiredArgsConstructor
public class RemoveCartItemUseCase {
    private final CartRepository cartRepository;

    public void execute(Long cartItemId) {
        // 1. 장바구니 아이템 존재 확인
        if (!cartRepository.findById(cartItemId).isPresent()) {
            throw new CartItemNotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 2. 삭제
        cartRepository.deleteById(cartItemId);
    }
}
