package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.user.User;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.memory.InMemoryUserRepository;
import com.ecommerce.presentation.dto.point.PointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GetPointUseCase 테스트")
class GetPointUseCaseTest {

    private GetPointUseCase getPointUseCase;
    private InMemoryUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        getPointUseCase = new GetPointUseCase(userRepository);
    }

    @Test
    @DisplayName("포인트 조회 성공")
    void getPointSuccess() {
        // given
        User user = userRepository.save(new User(null, "테스트 사용자", 100000L));
        Long userId = user.getId();

        // when
        PointResponse response = getPointUseCase.execute(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getPointBalance()).isEqualTo(100000L);
        assertThat(response.getLastUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 조회 시 예외 발생")
    void getPointWithInvalidUser() {
        // given
        Long invalidUserId = 999L;

        // when & then
        assertThatThrownBy(() -> getPointUseCase.execute(invalidUserId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
