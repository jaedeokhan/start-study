package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.user.User;

import java.util.Optional;

/**
 * User Repository Interface
 */
public interface UserRepository {
    Optional<User> findById(Long id);
    User save(User user);
    void chargeBalance(Long userId, long amount);
    void deductBalance(Long userId, long amount);
}
