package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import com.ecommerce.infrastructure.repository.UserRepository;
import com.ecommerce.presentation.dto.coupon.IssueCouponRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("쿠폰 선착순 발급 동시성 통합 테스트")
class CouponConcurrencyIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponEventRepository couponEventRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        // 테스트 간 격리를 위해 데이터 정리
        userCouponRepository.deleteAll();
        couponEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("100명이 50개 한정 쿠폰 선착순 발급 - 정확히 50명만 성공")
    void concurrentCouponIssuance_50Limit() throws InterruptedException {
        // given: 50개 한정 쿠폰 생성
        CouponEvent couponEvent = couponEventRepository.save(new CouponEvent(
                null,
                "선착순 50명 쿠폰",
                DiscountType.AMOUNT,
                10000L,
                50,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        ));

        // 100명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            users.add(userRepository.save(new User(null, "유저" + i, 100000L)));
        }

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(users.get(index).getId());
                    int status = mockMvc.perform(post("/api/v1/coupons/{couponEventId}/issue", couponEvent.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 정확히 50명만 성공
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);

        // DB 검증
        long issuedCount = userCouponRepository.count();
        assertThat(issuedCount).isEqualTo(50);

        CouponEvent result = couponEventRepository.findByIdOrThrow(couponEvent.getId());
        assertThat(result.getIssuedQuantity()).isEqualTo(50);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("200명이 100개 한정 쿠폰 선착순 발급 - 정확히 100명만 성공")
    void concurrentCouponIssuance_100Limit() throws InterruptedException {
        // given: 100개 한정 쿠폰 생성
        CouponEvent couponEvent = couponEventRepository.save(new CouponEvent(
                null,
                "선착순 100명 쿠폰",
                DiscountType.AMOUNT,
                5000L,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        ));

        // 200명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            users.add(userRepository.save(new User(null, "유저" + i, 100000L)));
        }

        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 200명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(users.get(index).getId());
                    int status = mockMvc.perform(post("/api/v1/coupons/{couponEventId}/issue", couponEvent.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 201) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 실패 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 정확히 100명만 성공
        assertThat(successCount.get()).isEqualTo(100);

        long issuedCount = userCouponRepository.count();
        assertThat(issuedCount).isEqualTo(100);

        CouponEvent result = couponEventRepository.findByIdOrThrow(couponEvent.getId());
        assertThat(result.getIssuedQuantity()).isEqualTo(100);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("소량 쿠폰 10개 한정 - 50명 동시 요청")
    void concurrentCouponIssuance_SmallQuantity() throws InterruptedException {
        // given: 10개 한정 쿠폰 생성
        CouponEvent couponEvent = couponEventRepository.save(new CouponEvent(
                null,
                "초소량 쿠폰",
                DiscountType.RATE,
                20L,
                10,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        ));

        // 50명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            users.add(userRepository.save(new User(null, "유저" + i, 100000L)));
        }

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 50명이 동시에 10개 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(users.get(index).getId());
                    int status = mockMvc.perform(post("/api/v1/coupons/{couponEventId}/issue", couponEvent.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 201) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 실패 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 정확히 10명만 성공
        assertThat(successCount.get()).isEqualTo(10);

        long issuedCount = userCouponRepository.count();
        assertThat(issuedCount).isEqualTo(10);

        CouponEvent result = couponEventRepository.findByIdOrThrow(couponEvent.getId());
        assertThat(result.getIssuedQuantity()).isEqualTo(10);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동일 사용자가 중복 발급 시도 - 1개만 발급")
    void concurrentCouponIssuance_DuplicateUser() throws InterruptedException {
        // given: 100개 쿠폰 생성
        CouponEvent couponEvent = couponEventRepository.save(new CouponEvent(
                null,
                "중복 발급 방지 테스트 쿠폰",
                DiscountType.AMOUNT,
                5000L,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        ));

        // 1명의 사용자 생성
        User user = userRepository.save(new User(null, "중복유저", 100000L));

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 동일 사용자가 50번 동시 발급 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    IssueCouponRequest request = new IssueCouponRequest(user.getId());
                    int status = mockMvc.perform(post("/api/v1/coupons/{couponEventId}/issue", couponEvent.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 201) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 실패 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);

        long issuedCount = userCouponRepository.countByUserId(user.getId());
        assertThat(issuedCount).isEqualTo(1);

        CouponEvent result = couponEventRepository.findByIdOrThrow(couponEvent.getId());
        assertThat(result.getIssuedQuantity()).isEqualTo(1);
    }
}
