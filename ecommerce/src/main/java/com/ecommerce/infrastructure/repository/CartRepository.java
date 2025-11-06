package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.cart.CartItem;

import java.util.List;
import java.util.Optional;

/**
 * Cart Repository Interface
 */
public interface CartRepository {
    Optional<CartItem> findById(Long id);
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    CartItem save(CartItem cartItem);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
}
