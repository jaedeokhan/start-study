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
        long mockUser = 1L;
        long mockPoint = 1000;
        UserPoint mockUserPoint = new UserPoint(mockUser, mockPoint, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(mockUserPoint);

        // when
        UserPoint userPoint = pointService.getPointById(mockUser);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(mockUser);
        assertThat(userPoint.point()).isEqualTo(mockPoint);
    }

    @Test
    void 존재하지않는_사용자_포인트_조회_테스트() {
        // given
        long mockUser = 0L;
        long mockPoint = 0;
        UserPoint mockUserPoint = UserPoint.empty(mockUser);
        when(userPointTable.selectById(mockUser)).thenReturn(mockUserPoint);

        // when
        UserPoint userPoint = pointService.getPointById(mockUser);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(mockUser);
        assertThat(userPoint.point()).isEqualTo(mockPoint);
    }
}