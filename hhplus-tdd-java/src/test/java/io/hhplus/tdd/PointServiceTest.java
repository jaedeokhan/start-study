package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @InjectMocks
    private PointService pointService;

    @Test
    void 사용자_포인트_조회_테스트(){
        // given
        UserPoint mockUser = new UserPoint(1L, 1000, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(mockUser);

        // when
        UserPoint userPoint = pointService.getPointById(1L);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(1L);
        assertThat(userPoint.point()).isEqualTo(1000);
    }

    @Test
    void 존재하지않는_사용자_포인트_조회_테스트() {
        // given
        UserPoint mockUser = UserPoint.empty(0L);
        when(userPointTable.selectById(0L)).thenReturn(mockUser);

        // when
        UserPoint userPoint = pointService.getPointById(0L);

        // then
            assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(0L);
        assertThat(userPoint.point()).isEqualTo(0);
    }
}
