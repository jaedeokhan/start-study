package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("존재하는 사용자의 포인트 조회")
    void getUserPoint_Success() {
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
    @DisplayName("존재하지 않는 사용자 조회 시 빈 포인트 반환")
    void getUserPoint_ReturnsEmpty_WhenUserNotFound() {
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
    @DisplayName("포인트 내역 조회 성공")
    void getPointHistories_Success() {
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
    @DisplayName("포인트 내역이 없을 때 빈 리스트 반환")
    void getPointHistories_ReturnsEmptyList_WhenNoHistory() {
        // given
        long userId = 0L;
        List<PointHistory> mockPointHistoryList = List.of();
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockPointHistoryList);

        // when
        List<PointHistory> histories = pointService.getHistoriesById(userId);

        // then
        assertThat(histories.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("포인트 충전 성공 및 내역 저장")
    void chargePointAndSaveHistory_Success() {
        // given
        long userId = 1L;
        long chargeAmount = 1000;
        UserPoint chargedUserPoint = UserPoint.empty(userId).charge(chargeAmount);
        when(userPointTable.selectById(userId)).thenReturn(chargedUserPoint);
        when(userPointTable.insertOrUpdate(anyLong(), anyLong()))
                .thenReturn(chargedUserPoint);

        // when
        UserPoint actualUserPoint = pointService.charge(userId, chargeAmount);

        // then
        assertThat(actualUserPoint.point()).isEqualTo(1000);
        verify(pointHistoryTable, times(1)).insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );
    }

    @Test
    @DisplayName("포인트 사용 성공 및 내역 저장")
    void usePointAbdSaveHistory_Success() {
        // given
        long userId = 1L;
        long amount = 1000;
        long useAmount = 500;
        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        UserPoint usedUserPoint = userPoint.use(useAmount);
        when(userPointTable.selectById(userId)).thenReturn(usedUserPoint);
        when(userPointTable.insertOrUpdate(anyLong(), anyLong()))
                .thenReturn(usedUserPoint);

        // when
        UserPoint actualUserPoint = pointService.use(userId, useAmount);

        // then
        assertThat(actualUserPoint.point()).isEqualTo(useAmount);
        verify(pointHistoryTable, times(1)).insert(
                eq(userId),
                eq(useAmount),
                eq(TransactionType.USE),
                anyLong()
        );
    }
}
