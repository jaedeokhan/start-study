package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>  {

    default Product findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new ProductNotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    default List<Product> findAll(int page, int size) {
        return findAll(PageRequest.of(page, size)).getContent();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    Product save(Product product);

    default int getTotalCount() { return (int) count(); }

    default void decreaseStock(Long productId, int quantity) {
        Product product = findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.decreaseStock(quantity);
    }}
