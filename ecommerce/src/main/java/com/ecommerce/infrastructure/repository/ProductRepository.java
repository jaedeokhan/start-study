package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.product.Product;

import java.util.List;
import java.util.Optional;

/**
 * Product Repository Interface
 */
public interface ProductRepository {
    Optional<Product> findById(Long id);
    List<Product> findAll(int page, int size);
    List<Product> findAllById(List<Long> ids);
    Product save(Product product);
    int getTotalCount();
    void decreaseStock(Long productId, int quantity);
}
