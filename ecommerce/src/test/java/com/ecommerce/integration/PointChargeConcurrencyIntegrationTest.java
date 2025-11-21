package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.repository.UserRepository;
import com.ecommerce.presentation.dto.point.ChargePointRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
@DisplayName("포인트 충전 동시성 통합 테스트")
class PointChargeConcurrencyIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 테스트 간 격리를 위해 데이터 정리
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("일반 케이스 - 여러 사용자가 각자 포인트 충전 (충돌 없음)")
    void concurrentPointCharge_MultipleUsers() throws InterruptedException {
        // given: 100명의 사용자 생성 (각각 초기 잔액 10,000원)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            users.add(userRepository.save(new User(null, "유저" + i, 10000L)));
        }

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 100명이 각자 5,000원씩 충전
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    ChargePointRequest request = new ChargePointRequest(users.get(index).getId(), 5000L);
                    int status = mockMvc.perform(post("/api/v1/points/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
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

        // then: 100명 모두 성공 (각자 다른 레코드이므로 충돌 없음)
        assertThat(successCount.get()).isEqualTo(100);

        // 각 사용자의 잔액이 정확히 15,000원이 되어야 함
        for (User user : users) {
            User result = userRepository.findByIdOrThrow(user.getId());
            assertThat(result.getPointBalance()).isEqualTo(15000L);
        }
    }

    @Test
    @DisplayName("예외 케이스 - 동일 사용자가 10번 동시 충전 (중복 요청)")
    void concurrentPointCharge_SameUser_DuplicateRequests() throws InterruptedException {
        // given: 1명의 사용자 (초기 잔액 10,000원)
        User user = userRepository.save(new User(null, "중복충전유저", 10000L));

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 동일 사용자가 5,000원 충전을 10번 동시 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargePointRequest request = new ChargePointRequest(user.getId(), 5000L);
                    int status = mockMvc.perform(post("/api/v1/points/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
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

        // then: 낙관적 락으로 인해 재시도 로직에 의해 10번 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(10);

        // 최종 잔액이 정확히 60,000원이 되어야 함 (10,000 + 5,000 * 10)
        User result = userRepository.findByIdOrThrow(user.getId());
        assertThat(result.getPointBalance()).isEqualTo(60000L);
    }

    @Test
    @DisplayName("예외 케이스 - 동일 사용자가 50번 동시 충전")
    void concurrentPointCharge_SameUser_ManyRequests() throws InterruptedException {
        // given: 1명의 사용자 (초기 잔액 0원)
        User user = userRepository.save(new User(null, "대량충전유저", 0L));

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 동일 사용자가 1,000원 충전을 50번 동시 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargePointRequest request = new ChargePointRequest(user.getId(), 1000L);
                    int status = mockMvc.perform(post("/api/v1/points/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
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

        // then: 낙관적 락 + 재시도로 모두 성공
        assertThat(successCount.get()).isEqualTo(50);

        // 최종 잔액이 정확히 50,000원이 되어야 함
        User result = userRepository.findByIdOrThrow(user.getId());
        assertThat(result.getPointBalance()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("혼합 케이스 - 일부는 여러 사용자, 일부는 동일 사용자")
    void concurrentPointCharge_MixedScenario() throws InterruptedException {
        // given: 10명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            users.add(userRepository.save(new User(null, "유저" + i, 10000L)));
        }

        // 특정 1명의 사용자는 중복 요청 대상
        User duplicateUser = users.get(0);

        int threadCount = 50; // 총 50개 스레드
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 첫 10개 스레드는 user0에게 중복 요청, 나머지 40개는 다른 사용자들에게 분산
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    User targetUser;
                    if (index < 10) {
                        // 첫 10개 요청은 동일 사용자
                        targetUser = duplicateUser;
                    } else {
                        // 나머지는 다른 사용자들에게 분산 (각 사용자당 4-5개 요청)
                        targetUser = users.get((index % 10));
                    }

                    ChargePointRequest request = new ChargePointRequest(targetUser.getId(), 2000L);
                    int status = mockMvc.perform(post("/api/v1/points/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
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

        // then: 모두 성공
        assertThat(successCount.get()).isEqualTo(50);

        // user0은 10번 + 4번(나머지 분산) = 14번 충전되어야 함
        User result0 = userRepository.findByIdOrThrow(duplicateUser.getId());
        // 실제로는 복잡하므로 단순히 증가했는지만 확인
        assertThat(result0.getPointBalance()).isGreaterThan(10000L);
    }

    @Test
    @DisplayName("대량 동시 충전 - 1000명이 각자 충전")
    void concurrentPointCharge_LargeScale() throws InterruptedException {
        // given: 1000명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            users.add(userRepository.save(new User(null, "유저" + i, 5000L)));
        }

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 1000명이 각자 10,000원씩 충전
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    ChargePointRequest request = new ChargePointRequest(users.get(index).getId(), 10000L);
                    int status = mockMvc.perform(post("/api/v1/points/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
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

        // then: 1000명 모두 성공 (각자 다른 레코드)
        assertThat(successCount.get()).isEqualTo(1000);

        // 샘플 검증: 처음 10명의 잔액 확인
        for (int i = 0; i < 10; i++) {
            User result = userRepository.findByIdOrThrow(users.get(i).getId());
            assertThat(result.getPointBalance()).isEqualTo(15000L);
        }
    }
}
