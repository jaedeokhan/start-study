package com.ecommerce.infrastructure.jpa.impl;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.infrastructure.jpa.CartJpaRepository;
import com.ecommerce.infrastructure.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartJpaRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public Optional<CartItem> findById(Long id) {
        return cartJpaRepository.findById(id);
    }

    @Override
    public List<CartItem> findByUserId(Long userId) {
        return cartJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return cartJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public CartItem save(CartItem cartItem) {
        return cartJpaRepository.save(cartItem);
    }

    @Override
    public void deleteById(Long id) {
        cartJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        cartJpaRepository.deleteByUserId(userId);
    }

}
