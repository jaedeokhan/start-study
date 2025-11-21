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
@DisplayName("재고 차감 동시성 통합 테스트")
class StockConcurrencyIntegrationTest extends TestContainerConfig {

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
    @DisplayName("100명이 재고 50개 상품 구매 - 정확히 50명만 성공")
    void concurrentStockDecrease_50Stock() throws InterruptedException {
        // given: 재고 50개 상품 생성
        Product product = productRepository.save(new Product(
                null,
                "인기 상품",
                "재고 50개 한정",
                10000L,
                50
        ));

        // 100명의 사용자 생성 (각각 충분한 포인트 보유)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 100000L));
            users.add(user);
            // 각 사용자의 장바구니에 상품 1개 추가
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명이 동시에 주문 (재고 차감)
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

        // then: 정확히 50명만 주문 성공
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);

        // DB 검증: 재고가 정확히 0이 되어야 함
        Product result = productRepository.findByIdOrThrow(product.getId());
        assertThat(result.getStock()).isEqualTo(0);

        // 주문 건수 검증
        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(50);
    }

    @Test
    @DisplayName("200명이 재고 100개 상품 구매 - 정확히 100명만 성공")
    void concurrentStockDecrease_100Stock() throws InterruptedException {
        // given: 재고 100개 상품 생성
        Product product = productRepository.save(new Product(
                null,
                "베스트셀러",
                "재고 100개",
                20000L,
                100
        ));

        // 200명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 100000L));
            users.add(user);
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 200명이 동시에 주문
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

        // then: 정확히 100명만 성공
        assertThat(successCount.get()).isEqualTo(100);

        Product result = productRepository.findByIdOrThrow(product.getId());
        assertThat(result.getStock()).isEqualTo(0);

        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(100);
    }

    @Test
    @DisplayName("소량 재고 10개 - 50명 동시 주문")
    void concurrentStockDecrease_SmallStock() throws InterruptedException {
        // given: 재고 10개만 있는 한정판 상품
        Product product = productRepository.save(new Product(
                null,
                "한정판 상품",
                "재고 10개 한정",
                50000L,
                10
        ));

        // 50명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 100000L));
            users.add(user);
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 50명이 동시에 10개 상품 주문
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

        // then: 정확히 10명만 성공
        assertThat(successCount.get()).isEqualTo(10);

        Product result = productRepository.findByIdOrThrow(product.getId());
        assertThat(result.getStock()).isEqualTo(0);

        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(10);
    }

    @Test
    @DisplayName("재고 음수 방지 검증 - 1개 재고에 3명 동시 주문")
    void concurrentStockDecrease_PreventNegativeStock() throws InterruptedException {
        // given: 재고 1개만 남은 상품
        Product product = productRepository.save(new Product(
                null,
                "마지막 상품",
                "재고 1개",
                30000L,
                1
        ));

        // 3명의 사용자 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 100000L));
            users.add(user);
            cartRepository.save(new CartItem(null, user.getId(), product.getId(), 1));
        }

        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 3명이 동시에 주문
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

        // then: 1명만 성공, 재고는 절대 음수가 되지 않음
        assertThat(successCount.get()).isEqualTo(1);

        Product result = productRepository.findByIdOrThrow(product.getId());
        assertThat(result.getStock()).isEqualTo(0);
        assertThat(result.getStock()).isGreaterThanOrEqualTo(0); // 음수 아님 명시적 검증

        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 상품 동시 주문 - 각 상품의 재고가 독립적으로 차감")
    void concurrentStockDecrease_MultipleProducts() throws InterruptedException {
        // given: 2개의 상품 생성
        Product product1 = productRepository.save(new Product(
                null, "상품1", "재고 20개", 10000L, 20
        ));
        Product product2 = productRepository.save(new Product(
                null, "상품2", "재고 30개", 15000L, 30
        ));

        // 50명의 사용자 생성 (25명씩 다른 상품 주문)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            User user = userRepository.save(new User(null, "유저" + i, 100000L));
            users.add(user);

            // 홀수 사용자: product1, 짝수 사용자: product2
            if (i % 2 == 1) {
                cartRepository.save(new CartItem(null, user.getId(), product1.getId(), 1));
            } else {
                cartRepository.save(new CartItem(null, user.getId(), product2.getId(), 1));
            }
        }

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 50명이 동시에 주문 (25명씩 다른 상품)
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

        // then: 45명 성공 (상품1: 20명, 상품2: 25명)
        assertThat(successCount.get()).isEqualTo(45);

        // 각 상품의 재고가 정확히 차감됨
        Product result1 = productRepository.findByIdOrThrow(product1.getId());
        Product result2 = productRepository.findByIdOrThrow(product2.getId());

        assertThat(result1.getStock()).isEqualTo(0); // 재고 20개 모두 소진
        assertThat(result2.getStock()).isEqualTo(5); // 5개 남음 (30 - 25)

        // 주문 건수 검증
        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(45);
    }
}
