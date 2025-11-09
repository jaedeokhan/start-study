package com.ecommerce.application.usecase.point;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import com.ecommerce.infrastructure.repository.InMemoryPointHistoryRepository;
import com.ecommerce.presentation.dto.point.PointHistoryListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GetPointHistoryUseCase 테스트")
class GetPointHistoryUseCaseTest {

    private GetPointHistoryUseCase getPointHistoryUseCase;
    private InMemoryPointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        pointHistoryRepository = new InMemoryPointHistoryRepository();
        getPointHistoryUseCase = new GetPointHistoryUseCase(pointHistoryRepository);
    }

    @Test
    @DisplayName("포인트 이력 조회 성공")
    void getPointHistorySuccess() {
        // given
        Long userId = 1L;
        pointHistoryRepository.save(new PointHistory(null, userId, 10000L, TransactionType.CHARGE, 110000L, "충전"));
        pointHistoryRepository.save(new PointHistory(null, userId, -5000L, TransactionType.USE, 105000L, 100L, "사용"));
        pointHistoryRepository.save(new PointHistory(null, userId, 20000L, TransactionType.CHARGE, 125000L, "충전"));

        // when
        PointHistoryListResponse response = getPointHistoryUseCase.execute(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getHistories()).hasSize(3);
        assertThat(response.getTotalCount()).isEqualTo(3);

        // 최신순 정렬 확인 (마지막에 저장한 것이 첫 번째)
        assertThat(response.getHistories().get(0).getTransactionType()).isEqualTo(TransactionType.CHARGE);
        assertThat(response.getHistories().get(0).getPointAmount()).isEqualTo(20000L);
    }

    @Test
    @DisplayName("포인트 이력이 없으면 빈 목록 반환")
    void getPointHistoryEmpty() {
        // given
        Long userId = 999L;

        // when
        PointHistoryListResponse response = getPointHistoryUseCase.execute(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getHistories()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션을 통한 포인트 이력 조회")
    void getPointHistoryWithPagination() {
        // given
        Long userId = 1L;
        for (int i = 0; i < 10; i++) {
            pointHistoryRepository.save(new PointHistory(
                null, userId, 1000L, TransactionType.CHARGE, 100000L + (i * 1000), "충전 " + i
            ));
        }

        // when
        PointHistoryListResponse response = getPointHistoryUseCase.executeWithPagination(userId, 0, 5);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getHistories()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(5);
    }
}
