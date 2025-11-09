package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.infrastructure.repository.CartRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Cart InMemory Repository 구현체
 * - ConcurrentHashMap 기반 인메모리 저장소
 */
@Repository
public class InMemoryCartRepository implements CartRepository {
    private final Map<Long, CartItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CartItem> findByUserId(Long userId) {
        return store.values().stream()
            .filter(item -> item.getUserId().equals(userId))
            .sorted(Comparator.comparing(CartItem::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return store.values().stream()
            .filter(item -> item.getUserId().equals(userId) && item.getProductId().equals(productId))
            .findFirst();
    }

    @Override
    public CartItem save(CartItem cartItem) {
        if (cartItem.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            CartItem newItem = new CartItem(
                newId,
                cartItem.getUserId(),
                cartItem.getProductId(),
                cartItem.getQuantity()
            );
            store.put(newId, newItem);
            return newItem;
        } else {
            store.put(cartItem.getId(), cartItem);
            return cartItem;
        }
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        List<Long> toDelete = store.values().stream()
            .filter(item -> item.getUserId().equals(userId))
            .map(CartItem::getId)
            .collect(Collectors.toList());

        toDelete.forEach(store::remove);
    }
}
