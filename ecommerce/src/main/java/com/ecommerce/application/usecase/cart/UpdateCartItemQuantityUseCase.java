package com.ecommerce.application.usecase.cart;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.cart.UpdateCartItemResponse;
import com.ecommerce.domain.cart.exception.CartItemNotFoundException;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.infrastructure.repository.CartRepository;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-CART-003: 장바구니 상품 수량 변경
 */
@Component
@RequiredArgsConstructor
public class UpdateCartItemQuantityUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public UpdateCartItemResponse execute(Long cartItemId, int quantity) {
        // 1. 장바구니 아이템 조회
        CartItem cartItem = cartRepository.findById(cartItemId)
            .orElseThrow(() -> new CartItemNotFoundException("장바구니 아이템을 찾을 수 없습니다: " + cartItemId));

        // 2. 상품 조회
        Product product = productRepository.findById(cartItem.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다: " + cartItem.getProductId()));

        // 3. 재고 확인
        if (!product.hasStock(quantity)) {
            throw new InsufficientStockException(
                String.format("재고 부족: 상품 '%s' (요청: %d, 재고: %d)",
                    product.getName(), quantity, product.getStock())
            );
        }

        // 4. 수량 변경 (Entity 비즈니스 로직 호출)
        cartItem.updateQuantity(quantity);

        // 5. 저장
        CartItem updatedItem = cartRepository.save(cartItem);

        // 6. 응답 생성
        return UpdateCartItemResponse.from(updatedItem, product);
    }
}
