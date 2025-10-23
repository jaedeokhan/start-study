package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.apache.catalina.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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
    @DisplayName("사용자가 존재할 때 사용자 포인트를 반환해야 한다.")
    void should_returnUserPoint_when_userExists() {
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
    @DisplayName("사용자가 존재하지 않을 때 사용자 포인트 빈 객체를 반환해야 한다.")
    void should_returnEmptyUserPoint_when_userDoesNotExist() {
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
    @DisplayName("사용자가 포인트 내역이 존재할 때 포인트 내역 리스트를 반환해야 한다.")
    void should_returnPointHistories_when_userHasPointHistory() {
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
    @DisplayName("사용자가 포인트 내역이 없을 때 빈 리스트를 반환해야 한다.")
    void should_returnEmptyList_when_userHasNoPointHistories() {
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
    @DisplayName("사용자 포인트가 충전되었을 때 포인트가 증가해야 한다.")
    void UserPoint_should_increasePoints_when_charged(int chargeAmount) {
        // given
        UserPoint userPoint = UserPoint.empty(1L);

        // when
        UserPoint chargedUserPoint = userPoint.charge(chargeAmount);

        // then
        assertThat(chargedUserPoint.point()).isEqualTo(chargeAmount);
    }

    @ParameterizedTest(name = "{0}원 사용 시 금액 {0}원 감소한다.")
    @ValueSource(ints = {2000, 3000})
    @DisplayName("사용자 포인트를 사용했을 때 포인트가 감소해야 한다.")
    void UserPoint_should_decreasePoints_when_used(int useAmount) {
        // given
        UserPoint userPoint = new UserPoint(1L, useAmount, System.currentTimeMillis());

        // when
        UserPoint usedUserPoint = userPoint.use(useAmount);

        // then
        assertThat(usedUserPoint.point()).isEqualTo(0);
    }
}
