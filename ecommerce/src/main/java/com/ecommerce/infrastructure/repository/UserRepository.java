package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.user.User;

import java.util.Optional;

/**
 * User Repository Interface
 */
public interface UserRepository {
    Optional<User> findById(Long id);
    User save(User user);
    void chargePoint(Long userId, long amount);
    void usePoint(Long userId, long amount);
}
