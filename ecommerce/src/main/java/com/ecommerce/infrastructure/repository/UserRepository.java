package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.user.User;
import com.ecommerce.domain.user.exception.UserErrorCode;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    default User findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    default void chargePoint(Long userId, long amount) {
        User user = findByIdWithLock(userId)
                .orElseThrow(() ->new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));

        user.chargePoint(amount);
    }

    default void usePoint(Long userId, long amount) {
        User user = findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));

        user.usePoint(amount);
    }
}
