package com.ecommerce.application.usecase.cart;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.presentation.dto.cart.CartResponse;
import com.ecommerce.infrastructure.repository.CartRepository;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-CART-002: 장바구니 목록 조회
 */
@Component
@RequiredArgsConstructor
public class GetCartItemsUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartResponse execute(Long userId) {
        // 1. 장바구니 아이템 조회
        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        // 2. 상품 정보 조회 (Batch)
        List<Long> productIds = cartItems.stream()
            .map(CartItem::getProductId)
            .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        // 3. 응답 생성
        return CartResponse.from(userId, cartItems, productMap);
    }
}
