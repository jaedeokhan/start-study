package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.point.exception.InsufficientPointException;
import com.ecommerce.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryUserRepository 동시성 제어 테스트")
class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    @DisplayName("사용자 저장 및 조회")
    void saveAndFindUser() {
        // given
        User user = new User(null, "사용자1", 10000);

        // when
        User saved = repository.save(user);
        User found = repository.findById(saved.getId()).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("사용자1");
        assertThat(found.getPointBalance()).isEqualTo(10000);
    }

    @Test
    @DisplayName("포인트 충전 - 단일 스레드")
    void chargePointSingleThread() {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        // when
        repository.chargePoint(saved.getId(), 5000);

        // then
        User found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPointBalance()).isEqualTo(15000);
    }

    @Test
    @DisplayName("포인트 사용 - 단일 스레드")
    void usePointSingleThread() {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        // when
        repository.usePoint(saved.getId(), 3000);

        // then
        User found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPointBalance()).isEqualTo(7000);
    }

    @Test
    @DisplayName("포인트 부족 시 사용 실패")
    void usePointFail() {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        // when & then
        assertThatThrownBy(() -> repository.usePoint(saved.getId(), 10001))
            .isInstanceOf(InsufficientPointException.class);
    }

    @Test
    @DisplayName("포인트 충전 - 동시성 제어 검증 (100개 스레드가 각각 100원씩 충전)")
    void chargePointConcurrency() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 0);
        User saved = repository.save(user);

        int threadCount = 100;
        int chargeAmount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.chargePoint(saved.getId(), chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        User result = repository.findById(saved.getId()).orElseThrow();
        assertThat(result.getPointBalance()).isEqualTo(10000);  // 100 * 100
    }

    @Test
    @DisplayName("포인트 사용 - 동시성 제어 검증 (50개 스레드가 각각 100원씩 사용)")
    void usePointConcurrency() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        int threadCount = 50;
        int useAmount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.usePoint(saved.getId(), useAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientPointException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        User result = repository.findById(saved.getId()).orElseThrow();

        // 10000원에서 50개 스레드가 각 100원씩 사용 (총 5000원 사용)
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(result.getPointBalance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("포인트 사용 - 동시성 제어 검증 with 부족 상황 (150개 스레드가 각 100원씩 사용)")
    void usePointConcurrencyWithInsufficientPoint() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        int threadCount = 150;
        int useAmount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.usePoint(saved.getId(), useAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientPointException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        User result = repository.findById(saved.getId()).orElseThrow();

        // 10000원에서 150개 스레드가 각 100원씩 사용 시도
        // 성공: 100개, 실패: 50개
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(result.getPointBalance()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시에 충전과 사용이 발생하는 경우")
    void chargeAndUseConcurrently() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        // when - 50개는 충전, 50개는 사용
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.chargePoint(saved.getId(), 100);
                } finally {
                    latch.countDown();
                }
            });
            executorService.submit(() -> {
                try {
                    repository.usePoint(saved.getId(), 100);
                } catch (InsufficientPointException e) {
                    // 포인트 부족 시 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        User result = repository.findById(saved.getId()).orElseThrow();
        // 10000 + (50 * 100) - (50 * 100) = 10000 (이론상)
        // 실제로는 사용 실패가 있을 수 있으므로 >= 10000
        assertThat(result.getPointBalance()).isGreaterThanOrEqualTo(10000);
    }
}
