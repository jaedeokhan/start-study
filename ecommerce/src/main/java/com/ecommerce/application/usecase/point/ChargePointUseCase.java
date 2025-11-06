package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import com.ecommerce.domain.user.User;
import com.ecommerce.presentation.dto.point.ChargePointResponse;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import com.ecommerce.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * US-PAY-002: 포인트 충전
 */
@Component
@RequiredArgsConstructor
public class ChargePointUseCase {
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public ChargePointResponse execute(Long userId, long amount) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 충전 전 포인트 저장
        long previousBalance = user.getPointBalance();

        // 3. 포인트 충전 (동시성 제어는 Repository에서)
        userRepository.chargePoint(userId, amount);

        // 4. 충전 후 사용자 정보 재조회
        User updatedUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 5. 포인트 이력 저장
        PointHistory pointHistory = new PointHistory(
            null,
            userId,
            amount,
            TransactionType.CHARGE,
            updatedUser.getPointBalance(),
            String.format("포인트 충전: %d원", amount)
        );
        pointHistoryRepository.save(pointHistory);

        // 6. 응답 생성
        return ChargePointResponse.from(updatedUser, previousBalance, amount);
    }
}
