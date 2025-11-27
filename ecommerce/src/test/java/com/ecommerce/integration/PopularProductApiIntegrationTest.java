package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("인기 상품 API 통합 테스트")
class PopularProductApiIntegrationTest extends TestContainerConfig {

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

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    CacheManager cacheManager;

    private User testUser;
    private List<Product> products;

    @BeforeEach
    void setUp() {
        clearAllCaches();
        testUser = userRepository.save(new User(null, "테스트유저", 10_000_000L));

        // 상품 10개 생성
        products = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            products.add(productRepository.save(
                    new Product(null, "상품" + i, "설명" + i, 10000L * i, 1000)
            ));
        }
    }

    /**
     * CacheManager를 통한 캐시 초기화
     */
    private void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    System.out.println("🗑️ 캐시 초기화: " + cacheName);
                }
            });
        }
    }


    @Test
    @DisplayName("인기 상품 Top 5 조회")
    void getPopularProducts_Top5() throws Exception {
        // given: 상품별 주문 생성
        createOrders(products.get(0).getId(), 10, 5);  // 50개
        createOrders(products.get(1).getId(), 10, 4);  // 40개
        createOrders(products.get(2).getId(), 10, 3);  // 30개
        createOrders(products.get(3).getId(), 10, 2);  // 20개
        createOrders(products.get(4).getId(), 10, 1);  // 10개
        createOrders(products.get(5).getId(), 10, 1);  // 10개 (6위, 제외)

        // when & then
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products", hasSize(5)))
                .andExpect(jsonPath("$.data.products[0].name").value("상품1"))
                .andExpect(jsonPath("$.data.products[0].salesCount").value(50))
                .andExpect(jsonPath("$.data.products[1].salesCount").value(40))
                .andExpect(jsonPath("$.data.products[2].salesCount").value(30))
                .andExpect(jsonPath("$.data.products[3].salesCount").value(20));
    }

    @Test
    @DisplayName("주문이 없을 때 빈 리스트 반환")
    void getPopularProducts_NoOrders() throws Exception {
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products", hasSize(0)));
    }

    @Test
    @DisplayName("인기 상품 캐시 동작 확인 - 실행 시간 비교")
    void getPopularProducts_캐시_성능_비교() throws Exception {
        // given: 테스트 데이터 생성
        createOrders(products.get(0).getId(), 10, 5);
        createOrders(products.get(1).getId(), 10, 4);

        // when & then: 첫 번째 호출 (캐시 MISS - DB 조회)
        long start1 = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        long time1 = System.currentTimeMillis() - start1;

        // when & then: 두 번째 호출 (캐시 HIT - Redis 조회)
        long start2 = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        long time2 = System.currentTimeMillis() - start2;

        // when & then: 세 번째 호출 (캐시 HIT)
        long start3 = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        long time3 = System.currentTimeMillis() - start3;

        // 결과 출력
        System.out.println("=== 캐시 성능 비교 ===");
        System.out.println("첫 번째 호출 (MISS): " + time1 + "ms");
        System.out.println("두 번째 호출 (HIT):  " + time2 + "ms");
        System.out.println("세 번째 호출 (HIT):  " + time3 + "ms");
        System.out.println("성능 개선율: " + ((time1 - time2) * 100.0 / time1) + "%");

        // 검증: 캐시 HIT가 훨씬 빨라야 함
        assertThat(time2).isLessThan(time1 / 5);  // 5배 이상 빠름
        assertThat(time3).isLessThan(time1 / 5);
    }

    @Test
    @DisplayName("인기 상품 동시 요청 1000건 - 캐시 성능")
    void getPopularProducts_1000건_동시_요청_캐시_성능() throws Exception {
        // given
        createOrders(products.get(0).getId(), 10, 5);

        int threadCount = 10;
        int requestsPerThread = 100;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        List<Long> executionTimes = new CopyOnWriteArrayList<>();

        long start = System.currentTimeMillis();

        // when: 1000건 동시 요청
        for (int i = 0; i < totalRequests; i++) {
            final int requestNum = i;
            executor.submit(() -> {
                try {
                    long reqStart = System.currentTimeMillis();

                    mockMvc.perform(get("/api/v1/products/popular"))
                            .andExpect(status().isOk());

                    long reqElapsed = System.currentTimeMillis() - reqStart;
                    executionTimes.add(reqElapsed);

                    System.out.println("요청 " + (requestNum + 1) + ": " + reqElapsed + "ms");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - start;

        executor.shutdown();

        // then: 통계 분석
        Long firstRequest = executionTimes.get(0);
        double avgTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        Long maxTime = executionTimes.stream()
                .max(Long::compare)
                .orElse(0L);
        Long minTime = executionTimes.stream()
                .min(Long::compare)
                .orElse(0L);

        System.out.println("\n=== 동시 요청 1000건 통계 ===");
        System.out.println("총 처리 시간: " + totalTime + "ms");
        System.out.println("평균 응답시간: " + avgTime + "ms");
        System.out.println("최소 응답시간: " + minTime + "ms");
        System.out.println("최대 응답시간: " + maxTime + "ms");
        System.out.println("TPS: " + (totalRequests * 1000.0 / totalTime));

        // 캐시 사용 시 평균 응답시간이 빨라야 함
        assertThat(avgTime).isLessThan(50.0);  // 평균 50ms 이하
    }

    @Test
    @DisplayName("인기 상품 동시 요청 10000건 - 캐시 성능")
    void getPopularProducts_10_000건_동시_요청_캐시_성능() throws Exception {
        // given
        createOrders(products.get(0).getId(), 10, 5);

        int threadCount = 100;
        int requestsPerThread = 100;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        List<Long> executionTimes = new CopyOnWriteArrayList<>();

        long start = System.currentTimeMillis();

        // when: 10000건 동시 요청
        for (int i = 0; i < totalRequests; i++) {
            final int requestNum = i;
            executor.submit(() -> {
                try {
                    long reqStart = System.currentTimeMillis();

                    mockMvc.perform(get("/api/v1/products/popular"))
                            .andExpect(status().isOk());

                    long reqElapsed = System.currentTimeMillis() - reqStart;
                    executionTimes.add(reqElapsed);

                    System.out.println("요청 " + (requestNum + 1) + ": " + reqElapsed + "ms");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - start;

        executor.shutdown();

        // then: 통계 분석
        Long firstRequest = executionTimes.get(0);
        double avgTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        Long maxTime = executionTimes.stream()
                .max(Long::compare)
                .orElse(0L);
        Long minTime = executionTimes.stream()
                .min(Long::compare)
                .orElse(0L);

        System.out.println("\n=== 동시 요청 10000건 통계 ===");
        System.out.println("총 처리 시간: " + totalTime + "ms");
        System.out.println("평균 응답시간: " + avgTime + "ms");
        System.out.println("최소 응답시간: " + minTime + "ms");
        System.out.println("최대 응답시간: " + maxTime + "ms");
        System.out.println("TPS: " + (totalRequests * 1000.0 / totalTime));

        // 캐시 사용 시 평균 응답시간이 빨라야 함
        assertThat(avgTime).isLessThan(300.0);  // 평균 300ms 이하
    }

    /**
     * quantity개씩 count번 주문 생성
     */
    private void createOrders(Long productId, int quantity, int count) throws Exception {
        createOrdersForUser(testUser.getId(), productId, quantity, count);
    }

    private void createOrdersForUser(Long userId, Long productId, int quantity, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            // 기존 장바구니 항목 삭제 (중복 키 에러 방지)
            cartRepository.deleteByUserId(userId);

            cartRepository.save(new CartItem(null, userId, productId, quantity));
            CreateOrderRequest request = new CreateOrderRequest(userId, null);
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }


}
