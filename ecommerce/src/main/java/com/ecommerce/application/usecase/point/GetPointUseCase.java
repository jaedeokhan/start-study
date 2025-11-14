package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.user.User;
import com.ecommerce.presentation.dto.point.PointResponse;
import com.ecommerce.domain.user.exception.UserErrorCode;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-PAY-001: 포인트 조회
 */
@Component
@RequiredArgsConstructor
public class GetPointUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PointResponse execute(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND));

        return PointResponse.from(user);
    }
}
