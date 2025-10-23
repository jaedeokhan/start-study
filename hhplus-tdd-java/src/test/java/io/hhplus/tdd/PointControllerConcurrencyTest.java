package io.hhplus.tdd;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointControllerConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Test
    @DisplayName("단일 사용자 계속 충전 race condition 확인")
    void singleUserRaceCondition() throws InterruptedException {
        // given
        long userId = 1L;
        long initialPoint = 0L;
        long chargeAmount = 100L;
        int threadCount = 10; // 많은 스레드로 테스트

        userPointTable.insertOrUpdate(userId, initialPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint result = userPointTable.selectById(userId);
        long expectedPoint = initialPoint + (chargeAmount * threadCount);

        assertThat(result.point()).isEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("여러 사용자 동시 충전 - 병렬 처리")
    void multiUserRaceCondition() throws InterruptedException {
        // given
        int userCount = 10;
        int chargePerUser = 10; // 각 사용자당 10번 추엊ㄴ
        long chargeAmount = 100L;
        long initialPoint = 1000L;

        // 사용자 초기화
        for (long userId = 1; userId <= userCount; userId++) {
            userPointTable.insertOrUpdate(userId, initialPoint);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(userCount * chargePerUser);

        // when
        for (long userId = 1; userId <= userCount; userId++) {
            long finalUserId = userId;
            for (int j = 0; j < chargePerUser; j++) {
                executorService.submit(() -> {
                    try {
                        pointService.charge(finalUserId, chargeAmount);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();

        // then - 각 사용자별 확인
        for (long userId = 1; userId <= userCount; userId++) {
            UserPoint result = userPointTable.selectById(userId);
            long expectedPoint = initialPoint + (chargeAmount * chargePerUser);
            assertThat(result.point())
                .withFailMessage("User %d의 포인트가 일치하지 않음", userId)
                .isEqualTo(expectedPoint);
        }
    }

    @Test
    @DisplayName("단일 사용자 계속 사용 race condition 확인")
    void singleUserUseRaceCondition() throws InterruptedException {
        // given
        long userId = 1L;
        long initialPoint = 10000L; // 충분한 초기 포인트
        long useAmount = 100L;
        int threadCount = 10;

        userPointTable.insertOrUpdate(userId, initialPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint result = userPointTable.selectById(userId);
        long expectedPoint = initialPoint - (useAmount * threadCount);

        assertThat(result.point()).isEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("여러 사용자 동시 사용 - 병렬 처리")
    void multiUserUseRaceCondition() throws InterruptedException {
        // given
        int userCount = 10;
        int usePerUser = 10; // 각 사용자당 10번 사용
        long useAmount = 100L;
        long initialPoint = 10000L; // 충분한 초기 포인트

        // 사용자 초기화
        for (long userId = 1; userId <= userCount; userId++) {
            userPointTable.insertOrUpdate(userId, initialPoint);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(userCount * usePerUser);

        // when
        for (long userId = 1; userId <= userCount; userId++) {
            long finalUserId = userId;
            for (int j = 0; j < usePerUser; j++) {
                executorService.submit(() -> {
                    try {
                        pointService.use(finalUserId, useAmount);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();

        // then - 각 사용자별 확인
        for (long userId = 1; userId <= userCount; userId++) {
            UserPoint result = userPointTable.selectById(userId);
            long expectedPoint = initialPoint - (useAmount * usePerUser);
            assertThat(result.point())
                .withFailMessage("User %d의 포인트가 일치하지 않음. 예상: %d, 실제: %d",
                    userId, expectedPoint, result.point())
                .isEqualTo(expectedPoint);
        }
    }
}
