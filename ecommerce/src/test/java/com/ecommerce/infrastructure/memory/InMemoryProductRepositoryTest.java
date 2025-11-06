package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryProductRepository 동시성 제어 테스트")
class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    @DisplayName("상품 저장 및 조회")
    void saveAndFindProduct() {
        // given
        Product product = new Product(null, "상품1", "설명1", 10000, 100);

        // when
        Product saved = repository.save(product);
        Product found = repository.findById(saved.getId()).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("상품1");
    }

    @Test
    @DisplayName("재고 차감 - 단일 스레드")
    void decreaseStockSingleThread() {
        // given
        Product product = new Product(null, "상품1", "설명1", 10000, 100);
        Product saved = repository.save(product);

        // when
        repository.decreaseStock(saved.getId(), 30);

        // then
        Product found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 부족 시 차감 실패")
    void decreaseStockFail() {
        // given
        Product product = new Product(null, "상품1", "설명1", 10000, 50);
        Product saved = repository.save(product);

        // when & then
        assertThatThrownBy(() -> repository.decreaseStock(saved.getId(), 51))
            .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("재고 차감 - 동시성 제어 검증 (50개 스레드가 각각 2개씩 차감)")
    void decreaseStockConcurrency() throws InterruptedException {
        // given
        Product product = new Product(null, "상품1", "설명1", 10000, 100);
        Product saved = repository.save(product);

        int threadCount = 50;
        int decreaseAmount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.decreaseStock(saved.getId(), decreaseAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Product result = repository.findById(saved.getId()).orElseThrow();

        // 100개 재고에서 50개 스레드가 각 2개씩 차감 시도
        // 성공한 스레드 수 = 100 / 2 = 50개 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(result.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 차감 - 동시성 제어 검증 with 부족 상황 (100개 스레드가 각 2개씩 차감)")
    void decreaseStockConcurrencyWithInsufficientStock() throws InterruptedException {
        // given
        Product product = new Product(null, "상품1", "설명1", 10000, 100);
        Product saved = repository.save(product);

        int threadCount = 100;  // 스레드가 재고보다 많음
        int decreaseAmount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.decreaseStock(saved.getId(), decreaseAmount);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Product result = repository.findById(saved.getId()).orElseThrow();

        // 100개 재고에서 100개 스레드가 각 2개씩 차감 시도
        // 성공: 50개, 실패: 50개
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);
        assertThat(result.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 상품에 대한 동시 재고 차감")
    void decreaseStockMultipleProducts() throws InterruptedException {
        // given
        Product product1 = repository.save(new Product(null, "상품1", "설명1", 10000, 100));
        Product product2 = repository.save(new Product(null, "상품2", "설명2", 20000, 100));
        Product product3 = repository.save(new Product(null, "상품3", "설명3", 30000, 100));

        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * 3);

        // when - 각 상품에 대해 30개 스레드가 동시에 1개씩 차감
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    repository.decreaseStock(product1.getId(), 1);
                } finally {
                    latch.countDown();
                }
            });
            executorService.submit(() -> {
                try {
                    repository.decreaseStock(product2.getId(), 1);
                } finally {
                    latch.countDown();
                }
            });
            executorService.submit(() -> {
                try {
                    repository.decreaseStock(product3.getId(), 1);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(repository.findById(product1.getId()).orElseThrow().getStock()).isEqualTo(70);
        assertThat(repository.findById(product2.getId()).orElseThrow().getStock()).isEqualTo(70);
        assertThat(repository.findById(product3.getId()).orElseThrow().getStock()).isEqualTo(70);
    }
}
