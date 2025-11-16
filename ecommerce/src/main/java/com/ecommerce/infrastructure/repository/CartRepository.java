package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findById(Long id);

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    CartItem save(CartItem cartItem);

    void deleteById(Long id);

    void deleteByUserId(Long userId);
}
