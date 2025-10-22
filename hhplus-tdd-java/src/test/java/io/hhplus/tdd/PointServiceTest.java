package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Test
    void 사용자_포인트_조회_테스트() {
        // given
        long userId = 1L;
        long point = 1000;
        UserPoint mockUserPoint = new UserPoint(userId, point, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        // when
        UserPoint userPoint = pointService.getPointById(userId);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(1L);
        assertThat(userPoint.point()).isEqualTo(1000);
    }

    @Test
    void 실패_존재하지않는_사용자_포인트_조회_테스트() {
        // given
        long userId = 0L;
        UserPoint mockUserPoint = UserPoint.empty(userId);
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        // when
        UserPoint userPoint = pointService.getPointById(userId);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(0L);
        assertThat(userPoint.point()).isEqualTo(0);
    }

    @Test
    void 포인트_내역_조회_테스트() {
        // given
        long userId = 1L;
        List<PointHistory> mockPointHistoryList = List.of(
                new PointHistory(1, userId, 2000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2, userId, 500, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(3, userId, 1500, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockPointHistoryList);

        // when
        List<PointHistory> histories = pointService.getHistoriesById(userId);

        // then
        assertThat(histories.size()).isEqualTo(3);
    }

    @Test
    void 실패_존재하지않는_사용자의_포인트_내역_조회_테스트() {
        // given
        long userId = 0L;
        List<PointHistory> mockPointHistoryList = List.of();
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockPointHistoryList);

        // when
        List<PointHistory> histories = pointService.getHistoriesById(userId);

        // then
        assertThat(histories.size()).isEqualTo(0);
    }

    @ParameterizedTest(name = "{0}원 충전 시 금액 {0}원 증가한다.")
    @ValueSource(ints = {1200, 1500})
    void 포인트_충전_정상_증가_테스트(int amount) {
        // given
        UserPoint userPoint = UserPoint.empty(1L);

        // when
        UserPoint chargedUserPoint = userPoint.charge(amount);

        // then
        assertThat(chargedUserPoint.point()).isEqualTo(amount);
    }
}
