package com.ecommerce.application.usecase.cart;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.cart.UpdateCartItemResponse;
import com.ecommerce.domain.cart.exception.CartErrorCode;
import com.ecommerce.domain.cart.exception.CartItemNotFoundException;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
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
            .orElseThrow(() -> new CartItemNotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND));

        // 2. 상품 조회
        Product product = productRepository.findById(cartItem.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 3. 재고 확인
        if (!product.hasStock(quantity)) {
            throw new InsufficientStockException(ProductErrorCode.INSUFFICIENT_STOCK);
        }

        // 4. 수량 변경 (Entity 비즈니스 로직 호출)
        cartItem.updateQuantity(quantity);

        // 5. 저장
        CartItem updatedItem = cartRepository.save(cartItem);

        // 6. 응답 생성
        return UpdateCartItemResponse.from(updatedItem, product);
    }
}
