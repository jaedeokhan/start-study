package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.user.User;
import com.ecommerce.domain.user.exception.UserErrorCode;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    default User findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }
}