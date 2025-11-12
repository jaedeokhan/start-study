package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.user.User;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.memory.InMemoryUserRepository;
import com.ecommerce.infrastructure.memory.InMemoryPointHistoryRepository;
import com.ecommerce.presentation.dto.point.ChargePointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChargePointUseCase 테스트")
class ChargePointUseCaseTest {

    private ChargePointUseCase chargePointUseCase;
    private InMemoryUserRepository userRepository;
    private InMemoryPointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        pointHistoryRepository = new InMemoryPointHistoryRepository();
        chargePointUseCase = new ChargePointUseCase(userRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePointSuccess() {
        // given
        User user = userRepository.save(new User(null, "테스트 사용자", 100000L));
        Long userId = user.getId();
        long chargeAmount = 50000L;

        // when
        ChargePointResponse response = chargePointUseCase.execute(userId, chargeAmount);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getPreviousBalance()).isEqualTo(100000L);
        assertThat(response.getChargedAmount()).isEqualTo(50000L);
        assertThat(response.getCurrentBalance()).isEqualTo(150000L);

        // 포인트 이력 확인
        assertThat(pointHistoryRepository.findByUserId(userId)).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 충전 시 예외 발생")
    void chargePointWithInvalidUser() {
        // given
        Long invalidUserId = 999L;

        // when & then
        assertThatThrownBy(() -> chargePointUseCase.execute(invalidUserId, 10000L))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("충전 금액이 0 이하이면 예외 발생")
    void chargePointWithInvalidAmount() {
        // given
        User user = userRepository.save(new User(null, "테스트 사용자", 100000L));

        // when & then
        assertThatThrownBy(() -> chargePointUseCase.execute(user.getId(), 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("충전 금액은 0보다 커야 합니다.");
    }
}
