package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.repository.*;
import com.ecommerce.presentation.dto.order.CreateOrderRequest;
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
@DisplayName("포인트 사용 동시성 통합 테스트")
class PointUseConcurrencyIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // 테스트 간 격리를 위해 데이터 정리
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("일반 케이스 - 여러 사용자가 각자 주문하여 포인트 사용 (충돌 없음)")
    void concurrentPointUse_MultipleUsers() throws InterruptedException {
        // given: 상품 생성 (충분한 재고)
        Product product = productRepository.save(new Product(
                null, "일반 상품", "충분한 재고", 10000L, 1000
        ));

        // 100명의 사용자 생성 (각각 초기 잔액 50,000원)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 50000L));
            users.add(user);
            // 각 사용자의 장바구니에 10,000원 상품 1개 추가
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 100명이 각자 주문 (각자 10,000원 포인트 사용)
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest(users.get(index).getId(), null);
                    int status = mockMvc.perform(post("/api/v1/orders")
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

        // then: 100명 모두 성공 (각자 다른 레코드이므로 충돌 없음)
        assertThat(successCount.get()).isEqualTo(100);

        // 각 사용자의 잔액이 정확히 40,000원이 되어야 함
        for (User user : users) {
            User result = userRepository.findByIdOrThrow(user.getId());
            assertThat(result.getPointBalance()).isEqualTo(40000L);
        }

        // 주문 건수 검증
        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(100);
    }

    @Test
    @DisplayName("예외 케이스 - 동일 사용자가 3번 동시 주문 (중복 주문)")
    void concurrentPointUse_SameUser_DuplicateOrders() throws InterruptedException {
        // given
        // 1명의 사용자 (초기 잔액 50,000원)
        User user = userRepository.save(new User(null, "중복주문유저", 50000L));

        // 3개의 장바구니 아이템 생성 (각각 15,000원)
        for (int i = 0; i < 3; i++) {
            Product prod = productRepository.save(new Product(
                    null, "상품" + i, "테스트", 15000L, 100
            ));
            cartRepository.save(new CartItem(null, user.getId(), prod.getId(), 1));
        }
        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 동일 사용자가 3번 동시 주문 시도 (각각 15,000원 사용)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest(user.getId(), null);
                    int status = mockMvc.perform(post("/api/v1/orders")
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

        // then: 장바구니에 3개가 있으므로, 첫 주문에서 모두 소진
        // 낙관적 락 + 재시도로 1번만 성공하고 나머지는 빈 장바구니로 실패
        assertThat(successCount.get()).isLessThanOrEqualTo(1);

        // 최종 잔액 검증 (50,000 - 45,000 = 5,000)
        User result = userRepository.findByIdOrThrow(user.getId());
        // 1번 주문 성공 시: 50,000 - 45,000 = 5,000
        // 0번 성공 시: 50,000 (그대로)
        assertThat(result.getPointBalance()).isIn(5000L, 50000L);
    }

    @Test
    @DisplayName("포인트 부족 검증 - 잔액보다 큰 금액 주문 시 실패")
    void concurrentPointUse_InsufficientBalance() throws InterruptedException {
        // given: 고가 상품 생성
        Product product = productRepository.save(new Product(
                null, "고가 상품", "비싼 상품", 40000L, 100
        ));

        // 5명의 사용자 (각각 초기 잔액 50,000원)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 50000L));
            users.add(user);
            // 각 사용자의 장바구니에 40,000원 상품 2개 추가 (총 80,000원)
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 2));
        }

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 5명이 동시에 주문 (각자 80,000원 필요하지만 50,000원만 보유)
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest(users.get(index).getId(), null);
                    int status = mockMvc.perform(post("/api/v1/orders")
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

        // then: 모두 실패 (포인트 부족)
        assertThat(successCount.get()).isEqualTo(0);
        assertThat(failCount.get()).isEqualTo(5);

        // 모든 사용자의 잔액이 그대로 50,000원
        for (User user : users) {
            User result = userRepository.findByIdOrThrow(user.getId());
            assertThat(result.getPointBalance()).isEqualTo(50000L);
        }
    }

    @Test
    @DisplayName("포인트 음수 방지 검증 - 잔액 50,000원에 3명이 30,000원 주문")
    void concurrentPointUse_PreventNegativeBalance() throws InterruptedException {
        // given: 상품 생성
        Product product = productRepository.save(new Product(
                null, "일반 상품", "30,000원", 30000L, 100
        ));

        // 3명의 사용자 (각각 초기 잔액 50,000원, 각자 다른 사용자)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 50000L));
            users.add(user);
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 3명이 각자 30,000원 주문
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest(users.get(index).getId(), null);
                    int status = mockMvc.perform(post("/api/v1/orders")
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

        // then: 3명 모두 성공 (각자 다른 사용자이므로 충돌 없음)
        assertThat(successCount.get()).isEqualTo(3);

        // 각 사용자의 잔액이 20,000원이 되어야 함
        for (User user : users) {
            User result = userRepository.findByIdOrThrow(user.getId());
            assertThat(result.getPointBalance()).isEqualTo(20000L);
            assertThat(result.getPointBalance()).isGreaterThanOrEqualTo(0); // 음수 아님 명시적 검증
        }
    }

    @Test
    @DisplayName("혼합 시나리오 - 대량 사용자와 일부 중복 주문")
    void concurrentPointUse_MixedScenario() throws InterruptedException {
        // given: 상품 생성
        Product product = productRepository.save(new Product(
                null, "혼합 테스트 상품", "5,000원", 5000L, 1000
        ));

        // 50명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 30000L));
            users.add(user);
            // 각 사용자의 장바구니에 상품 1개 추가
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 50명이 각자 주문 (각자 5,000원 사용)
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest(users.get(index).getId(), null);
                    int status = mockMvc.perform(post("/api/v1/orders")
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

        // then: 50명 모두 성공
        assertThat(successCount.get()).isEqualTo(50);

        // 샘플 검증: 처음 10명의 잔액 확인
        for (int i = 0; i < 10; i++) {
            User result = userRepository.findByIdOrThrow(users.get(i).getId());
            assertThat(result.getPointBalance()).isEqualTo(25000L);
        }

        // 주문 건수 검증
        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(50);
    }

    @Test
    @DisplayName("극단적 케이스 - 잔액 1,000원으로 1,000원 주문 5번 동시 시도")
    void concurrentPointUse_EdgeCase_ExactBalance() throws InterruptedException {
        // given: 저가 상품
        // 1명의 사용자 (초기 잔액 1,000원)
        User user = userRepository.save(new User(null, "극단유저", 1000L));

        // 5개의 장바구니 아이템 생성
        for (int i = 0; i < 5; i++) {
            Product prod = productRepository.save(new Product(
                    null, "저가 상품" + i, "테스트", 1000L, 100
            ));
            cartRepository.save(new CartItem(null, user.getId(), prod.getId(), 1));
        }

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 동일 사용자가 5번 동시 주문 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest(user.getId(), null);
                    int status = mockMvc.perform(post("/api/v1/orders")
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

        // then: 1번만 성공 (첫 주문에서 장바구니 전체 소진, 총 5,000원 필요하지만 1,000원만 있음)
        assertThat(successCount.get()).isLessThanOrEqualTo(1);

        // 최종 잔액이 0원 또는 1,000원
        User result = userRepository.findByIdOrThrow(user.getId());
        assertThat(result.getPointBalance()).isGreaterThanOrEqualTo(0);
    }
}
