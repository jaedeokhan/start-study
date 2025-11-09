package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryCouponEventRepository 동시성 제어 테스트")
class InMemoryCouponEventRepositoryTest {

    private InMemoryCouponEventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCouponEventRepository();
    }

    @Test
    @DisplayName("쿠폰 이벤트 저장 및 조회")
    void saveAndFindCouponEvent() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );

        // when
        CouponEvent saved = repository.save(couponEvent);
        CouponEvent found = repository.findById(1L).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("신규 가입 쿠폰");
    }

    @Test
    @DisplayName("쿠폰 발급 - 단일 스레드")
    void issueCouponSingleThread() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        repository.save(couponEvent);

        // when
        repository.issueCoupon(1L);

        // then
        CouponEvent found = repository.findById(1L).orElseThrow();
        assertThat(found.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 수량 소진 시 발급 실패")
    void issueCouponFail() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            1,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        repository.save(couponEvent);
        repository.issueCoupon(1L);

        // when & then
        assertThatThrownBy(() -> repository.issueCoupon(1L))
            .isInstanceOf(CouponSoldOutException.class);
    }

    @Test
    @DisplayName("쿠폰 발급 - 동시성 제어 검증 (50개 스레드가 각각 발급)")
    void issueCouponConcurrency() throws InterruptedException {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            50,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        repository.save(couponEvent);

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.issueCoupon(1L);
                    successCount.incrementAndGet();
                } catch (CouponSoldOutException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CouponEvent result = repository.findById(1L).orElseThrow();

        // 50개 쿠폰을 50개 스레드가 발급 시도
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(result.getIssuedQuantity()).isEqualTo(50);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("쿠폰 발급 - 동시성 제어 검증 with 부족 상황 (100개 스레드가 50개 쿠폰 발급)")
    void issueCouponConcurrencyWithSoldOut() throws InterruptedException {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            50,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        repository.save(couponEvent);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.issueCoupon(1L);
                    successCount.incrementAndGet();
                } catch (CouponSoldOutException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CouponEvent result = repository.findById(1L).orElseThrow();

        // 50개 쿠폰을 100개 스레드가 발급 시도
        // 성공: 50개, 실패: 50개
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(result.getIssuedQuantity()).isEqualTo(50);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 쿠폰 이벤트에 대한 동시 발급")
    void issueCouponMultipleEvents() throws InterruptedException {
        // given
        CouponEvent event1 = new CouponEvent(
            1L, "쿠폰1", DiscountType.AMOUNT, 5000L, 30,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)
        );
        CouponEvent event2 = new CouponEvent(
            2L, "쿠폰2", DiscountType.AMOUNT, 3000L, 30,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)
        );
        CouponEvent event3 = new CouponEvent(
            3L, "쿠폰3", DiscountType.AMOUNT, 1000L, 30,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)
        );

        repository.save(event1);
        repository.save(event2);
        repository.save(event3);

        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * 3);

        // when - 각 쿠폰에 대해 30개 스레드가 동시에 발급
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.issueCoupon(1L);
                } finally {
                    latch.countDown();
                }
            });
            executorService.submit(() -> {
                try {
                    repository.issueCoupon(2L);
                } finally {
                    latch.countDown();
                }
            });
            executorService.submit(() -> {
                try {
                    repository.issueCoupon(3L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(repository.findById(1L).orElseThrow().getIssuedQuantity()).isEqualTo(30);
        assertThat(repository.findById(2L).orElseThrow().getIssuedQuantity()).isEqualTo(30);
        assertThat(repository.findById(3L).orElseThrow().getIssuedQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("대량 동시 요청 테스트 (1000개 스레드가 100개 쿠폰 발급)")
    void issueCouponMassiveConcurrency() throws InterruptedException {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "선착순 쿠폰",
            DiscountType.AMOUNT,
            10000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        repository.save(couponEvent);

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.issueCoupon(1L);
                    successCount.incrementAndGet();
                } catch (CouponSoldOutException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CouponEvent result = repository.findById(1L).orElseThrow();

        // 정확히 100개만 발급되어야 함
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(900);
        assertThat(result.getIssuedQuantity()).isEqualTo(100);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }
}
