package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.infrastructure.redis.CouponRedisRepository;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("비동기 쿠폰 선착순 발급 동시성 통합 테스트")
class AsyncCouponConcurrencyIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponEventRepository couponEventRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponRedisRepository couponRedisRepository;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        couponEventRepository.deleteAll();
    }

    @Test
    @DisplayName("대량 테스트: 10,000명이 5,000개 쿠폰 발급 시도")
    void load_test_10000_users_5000_coupons() throws Exception {
        // given
        int totalQuantity = 5000;
        int threadCount = 10000;
        int threadPoolSize = 200; // Thread pool 크기

        CouponEvent couponEvent = createCouponEvent("대량 테스트 5K", totalQuantity);
        couponRedisRepository.initializeCouponStock(couponEvent.getId(), totalQuantity);

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger soldOutCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        log.info("===== 부하 테스트 시작: {}명이 {}개 쿠폰 발급 시도 =====", threadCount, totalQuantity);
        long startTime = System.currentTimeMillis();

        // when: 10,000명이 동시에 5,000개 쿠폰 발급 시도
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    Map<String, Object> request = new HashMap<>();
                    request.put("userId", userId);

                    mockMvc.perform(post("/api/v1/coupons/{couponEventId}/asyncIssue", couponEvent.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());

                    successCount.incrementAndGet();

                    // 진행 상황 로그 (1000건마다)
                    if (successCount.get() % 1000 == 0) {
                        log.info("발급 진행 중... 성공: {}", successCount.get());
                    }

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("COUPON_SOLD_OUT")) {
                        soldOutCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                        if (errorCount.get() <= 10) { // 처음 10개 에러만 로그
                            log.error("발급 실패: userId={}, error={}", userId, e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.MINUTES); // 최대 5분 대기
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        Duration duration = Duration.ofMillis(endTime - startTime);

        // then: 검증 및 결과 출력
        assertThat(finished).isTrue(); // 시간 내 완료 확인

        log.info("===== 부하 테스트 완료 =====");
        log.info("총 소요 시간: {}초 ({}ms)", duration.getSeconds(), duration.toMillis());
        log.info("성공: {} / 재고부족: {} / 에러: {}", successCount.get(), soldOutCount.get(), errorCount.get());
        log.info("초당 처리량: {} TPS", (threadCount * 1000.0) / duration.toMillis());

        // Redis 검증
        int remainingStock = couponRedisRepository.getRemainingStock(couponEvent.getId());

        log.info("Redis 남은 재고: {}", remainingStock);

        // 검증
        assertThat(successCount.get()).isEqualTo(totalQuantity);
        assertThat(errorCount.get()).isEqualTo(0); // 에러 없어야 함
        assertThat(remainingStock).isEqualTo(0); // 재고 소진
    }

    @Test
    @DisplayName("극한 테스트: 20,000명이 5,000개 쿠폰 쟁탈전")
    void extreme_load_test_20000_users_5000_coupons() throws Exception {
        // given
        int totalQuantity = 5000;
        int threadCount = 20000;
        int threadPoolSize = 500;

        CouponEvent couponEvent = createCouponEvent("극한 테스트 쿠폰", totalQuantity);
        couponRedisRepository.initializeCouponStock(couponEvent.getId(), totalQuantity);

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        log.info("===== 극한 테스트 시작: {}명이 {}개 쿠폰 쟁탈전 =====", threadCount, totalQuantity);
        long startTime = System.currentTimeMillis();

        // when
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    Map<String, Object> request = new HashMap<>();
                    request.put("userId", userId);

                    mockMvc.perform(post("/api/v1/coupons/{couponEventId}/asyncIssue", couponEvent.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());

                    int current = successCount.incrementAndGet();
                    if (current % 500 == 0) {
                        log.info("발급 진행: {}/{}", current, totalQuantity);
                    }

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.MINUTES);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        Duration duration = Duration.ofMillis(endTime - startTime);

        // then
        assertThat(finished).isTrue();

        log.info("===== 극한 테스트 완료 =====");
        log.info("총 소요 시간: {}초", duration.getSeconds());
        log.info("성공: {} / 실패: {}", successCount.get(), failCount.get());
        log.info("성공률: {}%", (successCount.get() * 100.0) / threadCount);
        log.info("초당 처리량: {} TPS", (threadCount * 1000.0) / duration.toMillis());

        assertThat(successCount.get()).isEqualTo(totalQuantity);
        assertThat(couponRedisRepository.getRemainingStock(couponEvent.getId())).isEqualTo(0);
    }
    // Helper Method
    private CouponEvent createCouponEvent(String name, int totalQuantity) {
        return couponEventRepository.save(
                new CouponEvent(
                        null,
                        name,
                        DiscountType.AMOUNT,
                        5000L,
                        totalQuantity,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(30)
                )
        );
    }
}
