package com.ecommerce.application.usecase.point;

import com.ecommerce.application.lock.DistributedLock;
import com.ecommerce.application.lock.constant.LockType;
import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import com.ecommerce.domain.user.User;
import com.ecommerce.presentation.dto.point.ChargePointResponse;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import com.ecommerce.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-PAY-002: 포인트 충전
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargePointUseCase {
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @DistributedLock(key = "'user:charge:' + #userId", type = LockType.PUB_SUB)
    @Transactional
    public ChargePointResponse execute(Long userId, long amount) {
        log.debug("포인트 충전 시도: userId={}, amount={}", userId, amount);

        // 1. 사용자 조회 (낙관적 락)
        User user = userRepository.findByIdOrThrow(userId);

        // 2. 충전 전 포인트 저장
        long previousBalance = user.getPointBalance();

        // 3. 포인트 충전
        user.chargePoint(amount);

        // 4. 포인트 이력 저장
        PointHistory pointHistory = new PointHistory(
                null,
                userId,
                amount,
                TransactionType.CHARGE,
                user.getPointBalance(),
                String.format("포인트 충전: %d원", amount)
        );
        pointHistoryRepository.save(pointHistory);

        // 5. 응답 생성
        return ChargePointResponse.from(user, previousBalance, amount);
    }
}