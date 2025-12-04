package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.product.Product;
import com.ecommerce.infrastructure.repository.ProductRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Product API 통합 테스트")
class ProductApiIntegrationTest extends TestContainerConfig {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUpData() {
        clearAllCaches();
        // 테스트 데이터 준비
        productRepository.save(Product.create("노트북", "고성능 노트북", 1500000, 10));
        productRepository.save(Product.create("마우스", "무선 마우스", 35000, 50));
        productRepository.save(Product.create("키보드", "기계식 키보드", 120000, 30));
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
    @DisplayName("GET /api/v1/products - 상품 목록 조회 성공")
    void getProducts_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products", hasSize(3)))
                .andExpect(jsonPath("$.data.products[0].name").exists())
                .andExpect(jsonPath("$.data.products[0].price").exists())
                .andExpect(jsonPath("$.data.products[0].stock").exists())
                .andExpect(jsonPath("$.data.pagination.totalElements").value(3))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/products - 페이징 테스트")
    void getProducts_Paging() throws Exception {
        // given - 추가 상품 생성
        for (int i = 1; i <= 10; i++) {
            productRepository.save(Product.create("상품" + i, "설명", 10000, 10));
        }

        // when & then - 첫 페이지
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products", hasSize(5)))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.size").value(5));

        // when & then - 두 번째 페이지
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products", hasSize(5)));
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId} - 상품 단건 조회 성공")
    void getProduct_Success() throws Exception {
        // given
        Product product = productRepository.save(
                Product.create("테스트 상품", "테스트 설명", 50000, 20)
        );

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.description").value("테스트 설명"))
                .andExpect(jsonPath("$.data.price").value(50000))
                .andExpect(jsonPath("$.data.stock").value(20));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("상품 상세 조회 - 동시 요청 1000건 캐시 성능 테스트")
    void 상품_상세_조회_성능_테스트() throws Exception {
        // given
        Product product = productRepository.save(
                Product.create("테스트 상품", "테스트 설명", 50000, 100)
        );

        clearAllCaches();

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

                    mockMvc.perform(
                                    get("/api/v1/products/{productId}", product.getId())
                                            .contentType(MediaType.APPLICATION_JSON)
                            )
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.success").value(true));

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

        // then: 통계
        double avgTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long minTime = executionTimes.stream()
                .min(Long::compare)
                .orElse(0L);

        long maxTime = executionTimes.stream()
                .max(Long::compare)
                .orElse(0L);

        System.out.println("\n=== 상품 상세 조회 동시 요청 1000건 통계 ===");
        System.out.println("총 처리 시간: " + totalTime + "ms");
        System.out.println("평균 응답시간: " + avgTime + "ms");
        System.out.println("최소 응답시간: " + minTime + "ms");
        System.out.println("최대 응답시간: " + maxTime + "ms");
        System.out.println("TPS: " + (totalRequests * 1000.0 / totalTime));

        // 검증
        assertThat(avgTime).isLessThan(100.0);
    }
}
