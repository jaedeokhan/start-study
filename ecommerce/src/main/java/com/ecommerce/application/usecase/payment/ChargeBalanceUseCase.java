package com.ecommerce.application.usecase.payment;

import com.ecommerce.domain.user.User;
import com.ecommerce.presentation.dto.payment.ChargeBalanceResponse;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-PAY-001: 잔액 충전
 */
@Component
@RequiredArgsConstructor
public class ChargeBalanceUseCase {
    private final UserRepository userRepository;

    public ChargeBalanceResponse execute(Long userId, long amount) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 충전 전 잔액 저장
        long previousBalance = user.getBalance();

        // 3. 잔액 충전 (Entity 비즈니스 로직 + 동시성 제어는 Repository에서)
        userRepository.chargeBalance(userId, amount);

        // 4. 충전 후 사용자 정보 재조회
        User updatedUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 5. 응답 생성
        return ChargeBalanceResponse.from(updatedUser, previousBalance, amount);
    }
}
