package com.ecommerce.infrastructure.jpa.impl;

import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.infrastructure.jpa.ProductJpaRepository;
import com.ecommerce.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductJpaRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<Product> findAll(int page, int size) {
        return productJpaRepository.findAll(PageRequest.of(page, size)).getContent();

    }

    @Override
    public List<Product> findAllById(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public int getTotalCount() { return (int)productJpaRepository.count(); };

    @Override
    public void decreaseStock(Long productId, int quantity) {
        Product product = productJpaRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.decreaseStock(quantity);
    }
}
