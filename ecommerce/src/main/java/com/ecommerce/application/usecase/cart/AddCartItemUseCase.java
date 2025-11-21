package com.ecommerce.application.usecase.cart;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.cart.AddCartItemResponse;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.infrastructure.repository.CartRepository;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * US-CART-001: 장바구니에 상품 추가
 */
@Component
@RequiredArgsConstructor
public class AddCartItemUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional
    public AddCartItemResponse execute(Long userId, Long productId, int quantity) {
        // 1. 상품 조회
        Product product = productRepository.findByIdOrThrow(productId);

        // 2. 재고 확인
        if (!product.hasStock(quantity)) {
            throw new InsufficientStockException(ProductErrorCode.INSUFFICIENT_STOCK);
        }

        // 3. 기존 장바구니 아이템 확인
        CartItem cartItem;
        Optional<CartItem> existingItem = cartRepository.findByUserIdAndProductId(userId, productId);

        if (existingItem.isPresent()) {
            // 3-1. 기존 아이템이 있으면 수량 증가
            cartItem = existingItem.get();
            cartItem.updateQuantity(cartItem.getQuantity() + quantity);
        } else {
            // 3-2. 새로운 아이템 추가
            cartItem = new CartItem(null, userId, productId, quantity);
        }

        cartRepository.save(cartItem);

        // 4. 응답 생성
        return AddCartItemResponse.from(cartItem, product);
    }
}
