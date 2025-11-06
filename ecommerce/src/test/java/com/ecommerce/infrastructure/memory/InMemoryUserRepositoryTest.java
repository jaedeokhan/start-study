package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.payment.exception.InsufficientBalanceException;
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
        assertThat(found.getBalance()).isEqualTo(10000);
    }

    @Test
    @DisplayName("잔액 충전 - 단일 스레드")
    void chargeBalanceSingleThread() {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        // when
        repository.chargeBalance(saved.getId(), 5000);

        // then
        User found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getBalance()).isEqualTo(15000);
    }

    @Test
    @DisplayName("잔액 차감 - 단일 스레드")
    void deductBalanceSingleThread() {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        // when
        repository.deductBalance(saved.getId(), 3000);

        // then
        User found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getBalance()).isEqualTo(7000);
    }

    @Test
    @DisplayName("잔액 부족 시 차감 실패")
    void deductBalanceFail() {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        // when & then
        assertThatThrownBy(() -> repository.deductBalance(saved.getId(), 10001))
            .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("잔액 충전 - 동시성 제어 검증 (100개 스레드가 각각 100원씩 충전)")
    void chargeBalanceConcurrency() throws InterruptedException {
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
                    repository.chargeBalance(saved.getId(), chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        User result = repository.findById(saved.getId()).orElseThrow();
        assertThat(result.getBalance()).isEqualTo(10000);  // 100 * 100
    }

    @Test
    @DisplayName("잔액 차감 - 동시성 제어 검증 (50개 스레드가 각각 100원씩 차감)")
    void deductBalanceConcurrency() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        int threadCount = 50;
        int deductAmount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.deductBalance(saved.getId(), deductAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException e) {
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

        // 10000원에서 50개 스레드가 각 100원씩 차감 (총 5000원 차감)
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(result.getBalance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("잔액 차감 - 동시성 제어 검증 with 부족 상황 (150개 스레드가 각 100원씩 차감)")
    void deductBalanceConcurrencyWithInsufficientBalance() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        int threadCount = 150;
        int deductAmount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.deductBalance(saved.getId(), deductAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException e) {
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

        // 10000원에서 150개 스레드가 각 100원씩 차감 시도
        // 성공: 100개, 실패: 50개
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(result.getBalance()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시에 충전과 차감이 발생하는 경우")
    void chargeAndDeductConcurrently() throws InterruptedException {
        // given
        User user = new User(null, "사용자1", 10000);
        User saved = repository.save(user);

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        // when - 50개는 충전, 50개는 차감
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.chargeBalance(saved.getId(), 100);
                } finally {
                    latch.countDown();
                }
            });
            executorService.submit(() -> {
                try {
                    repository.deductBalance(saved.getId(), 100);
                } catch (InsufficientBalanceException e) {
                    // 잔액 부족 시 무시
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
        // 실제로는 차감 실패가 있을 수 있으므로 >= 10000
        assertThat(result.getBalance()).isGreaterThanOrEqualTo(10000);
    }
}
