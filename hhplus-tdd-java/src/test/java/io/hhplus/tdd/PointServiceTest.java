package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
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
    void 사용자_포인트_조회_테스트(){
        // given
        long mockUserId = 1L;
        long mockPoint = 1000;
        UserPoint mockUserPoint = new UserPoint(mockUserId, mockPoint, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(mockUserPoint);

        // when
        UserPoint userPoint = pointService.getPointById(mockUserId);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(mockUserId);
        assertThat(userPoint.point()).isEqualTo(mockPoint);
    }

    @Test
    void 실패_존재하지않는_사용자_포인트_조회_테스트() {
        // given
        long mockUserId = 0L;
        long mockPoint = 0;
        UserPoint mockUserPoint = UserPoint.empty(mockUserId);
        when(userPointTable.selectById(mockUserId)).thenReturn(mockUserPoint);

        // when
        UserPoint userPoint = pointService.getPointById(mockUserId);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(mockUserId);
        assertThat(userPoint.point()).isEqualTo(mockPoint);
    }

    @Test
    void 포인트_내역_조회_테스트() {
        // given
        long mockUserId = 1L;
        List<PointHistory> mockPointHistoryList = List.of(
                new PointHistory(1, mockUserId, 2000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2, mockUserId, 500, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(3, mockUserId, 1500, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(mockUserId)).thenReturn(mockPointHistoryList);

        // when
        List<PointHistory> histories = pointService.getHistoriesById(mockUserId);

        // then
        assertThat(histories.size()).isEqualTo(3);
    }

    @Test
    void 실패_존재하지않는_사용자의_포인트_내역_조회_테스트() {
        // given
        long mockUserId = 0L;
        List<PointHistory> mockPointHistoryList = List.of();
        when(pointHistoryTable.selectAllByUserId(mockUserId)).thenReturn(mockPointHistoryList);

        // when
        List<PointHistory> histories = pointService.getHistoriesById(mockUserId);

        // then
        assertThat(histories.size()).isEqualTo(0);
    }
}
