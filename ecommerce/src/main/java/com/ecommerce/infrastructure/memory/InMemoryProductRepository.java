package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.infrastructure.repository.ProductRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Product InMemory Repository 구현체
 * - ConcurrentHashMap 기반 인메모리 저장소
 * - ReentrantLock을 활용한 재고 차감 동시성 제어
 */
@Repository
@Profile("memory")
public class InMemoryProductRepository implements ProductRepository {
    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // ✅ 상품별 Lock 관리 (동시성 제어)
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll(int page, int size) {
        return store.values().stream()
            .sorted(Comparator.comparing(Product::getId))
            .skip((long) page * size)
            .limit(size)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findAllById(List<Long> ids) {
        return ids.stream()
            .map(store::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // 새로운 상품 생성
            Long newId = idGenerator.getAndIncrement();
            Product newProduct = new Product(
                newId,
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock()
            );
            store.put(newId, newProduct);
            return newProduct;
        } else {
            // 기존 상품 업데이트
            store.put(product.getId(), product);
            return product;
        }
    }

    @Override
    public int getTotalCount() {
        return store.size();
    }

    /**
     * ✅ ReentrantLock을 활용한 재고 차감 (동시성 제어)
     * Critical Section만 Lock 적용
     */
    @Override
    public void decreaseStock(Long productId, int quantity) {
        // 1. Lock 획득 (상품별 Lock)
        Lock lock = locks.computeIfAbsent(productId, k -> new ReentrantLock());
        lock.lock();

        try {
            // 2. Critical Section: 재고 차감
            Product product = store.get(productId);
            if (product == null) {
                throw new ProductNotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND);
            }

            // Entity의 비즈니스 로직 호출 (검증 포함)
            product.decreaseStock(quantity);

        } finally {
            // 3. Lock 해제 (반드시 finally에서)
            lock.unlock();
        }
    }
}
