package com.ecommerce.infrastructure.jpa.impl;

import com.ecommerce.domain.user.User;
import com.ecommerce.domain.user.exception.UserErrorCode;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.jpa.UserJpaRepository;
import com.ecommerce.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public void chargePoint(Long userId, long amount) {
        User user = userJpaRepository.findByIdWithLock(userId)
                .orElseThrow(() ->new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));

        user.chargePoint(amount);
    }

    @Override
    public void usePoint(Long userId, long amount) {
        User user = userJpaRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));

        user.usePoint(amount);
    }
}
