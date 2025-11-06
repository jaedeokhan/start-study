package com.ecommerce.application.usecase.payment;

import com.ecommerce.domain.user.User;
import com.ecommerce.presentation.dto.payment.BalanceResponse;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-PAY-002: 잔액 조회
 */
@Component
@RequiredArgsConstructor
public class GetBalanceUseCase {
    private final UserRepository userRepository;

    public BalanceResponse execute(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 응답 생성
        return BalanceResponse.from(user);
    }
}
