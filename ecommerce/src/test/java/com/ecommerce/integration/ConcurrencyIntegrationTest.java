package com.ecommerce.integration;

import com.ecommerce.application.usecase.coupon.IssueCouponUseCase;
import com.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.ecommerce.application.usecase.point.ChargePointUseCase;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.memory.*;
import com.ecommerce.infrastructure.repository.InMemoryPointHistoryRepository;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("동시성 제어 통합 테스트")
class ConcurrencyIntegrationTest {

    private IssueCouponUseCase issueCouponUseCase;
    private CreateOrderUseCase createOrderUseCase;
    private ChargePointUseCase chargePointUseCase;

    private InMemoryCouponEventRepository couponEventRepository;
    private InMemoryUserCouponRepository userCouponRepository;
    private InMemoryProductRepository productRepository;
    private InMemoryUserRepository userRepository;
    private InMemoryCartRepository cartRepository;
    private InMemoryOrderRepository orderRepository;
    private InMemoryPointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        couponEventRepository = new InMemoryCouponEventRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        productRepository = new InMemoryProductRepository();
        userRepository = new InMemoryUserRepository();
        cartRepository = new InMemoryCartRepository();
        orderRepository = new InMemoryOrderRepository();
        pointHistoryRepository = new InMemoryPointHistoryRepository();

        issueCouponUseCase = new IssueCouponUseCase(couponEventRepository, userCouponRepository);
        createOrderUseCase = new CreateOrderUseCase(
            cartRepository,
            productRepository,
            userRepository,
            userCouponRepository,
            couponEventRepository,
            orderRepository,
            pointHistoryRepository
        );
        chargePointUseCase = new ChargePointUseCase(userRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("동시성 시나리오 1: 100명이 50개 한정 쿠폰 선착순 발급")
    void concurrentCouponIssuance() throws InterruptedException {
        // given - 50개 한정 쿠폰
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "선착순 쿠폰",
            DiscountType.AMOUNT,
            10000L,
            50,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        couponEventRepository.save(couponEvent);

        // 100명의 사용자 생성
        for (long i = 1; i <= 100; i++) {
            userRepository.save(new User(i, "사용자" + i, 100000));
        }

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 쿠폰 발급 시도
        for (long i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    IssueCouponResponse response = issueCouponUseCase.execute(1L, userId);
                    if (response != null) {
                        successCount.incrementAndGet();
                    }
                } catch (CouponSoldOutException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // 기타 예외(발급 기간 문제 등)는 실패로 간주
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 50명만 성공, 50명은 실패
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);

        CouponEvent result = couponEventRepository.findById(1L).orElseThrow();
        assertThat(result.getIssuedQuantity()).isEqualTo(50);
        assertThat(result.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 시나리오 2: 100명이 50개 재고 상품 동시 구매")
    void concurrentProductPurchase() throws InterruptedException {
        // given - 50개 재고 상품
        Product product = productRepository.save(
            new Product(null, "인기 상품", "한정판", 10000, 50)
        );

        // 100명의 사용자 생성 및 장바구니에 상품 1개씩 담기
        for (long i = 1; i <= 100; i++) {
            User user = userRepository.save(new User(i, "사용자" + i, 20000));
            CartItem cartItem = new CartItem(null, user.getId(), product.getId(), 1);
            cartRepository.save(cartItem);
        }

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 주문 시도
        for (long i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    createOrderUseCase.execute(userId, null);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 50명만 성공, 50명은 재고 부족으로 실패
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);

        Product result = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 시나리오 3: 동일 사용자가 동시에 여러 번 포인트 충전")
    void concurrentPointCharge() throws InterruptedException {
        // given
        User user = userRepository.save(new User(1L, "사용자1", 0));

        int threadCount = 100;
        int chargeAmount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 동일 사용자가 100번 동시 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    chargePointUseCase.execute(1L, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 모든 충전이 정확히 반영되어야 함
        User result = userRepository.findById(1L).orElseThrow();
        assertThat(result.getPointBalance()).isEqualTo(100000);  // 1000 * 100
    }

    @Test
    @DisplayName("동시성 시나리오 4: 여러 사용자가 서로 다른 상품 동시 구매 (교착 상태 방지)")
    void concurrentMultipleProducts() throws InterruptedException {
        // given - 3개 상품 생성
        Product product1 = productRepository.save(
            new Product(null, "상품1", "설명1", 10000, 30)
        );
        Product product2 = productRepository.save(
            new Product(null, "상품2", "설명2", 20000, 30)
        );
        Product product3 = productRepository.save(
            new Product(null, "상품3", "설명3", 30000, 30)
        );

        // 90명의 사용자 생성 (각 상품당 30명)
        for (long i = 1; i <= 90; i++) {
            User user = userRepository.save(new User(i, "사용자" + i, 50000));

            // 30명씩 다른 상품을 장바구니에 담음
            Long productId;
            if (i <= 30) {
                productId = product1.getId();
            } else if (i <= 60) {
                productId = product2.getId();
            } else {
                productId = product3.getId();
            }

            CartItem cartItem = new CartItem(null, user.getId(), productId, 1);
            cartRepository.save(cartItem);
        }

        int threadCount = 90;
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when - 90명이 동시에 주문 시도
        for (long i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    createOrderUseCase.execute(userId, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외 발생 시 무시 (재고 부족 등)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 모든 주문이 성공해야 함 (교착 상태 없음)
        assertThat(successCount.get()).isEqualTo(90);

        // 모든 상품 재고가 0이어야 함
        assertThat(productRepository.findById(product1.getId()).orElseThrow().getStock()).isEqualTo(0);
        assertThat(productRepository.findById(product2.getId()).orElseThrow().getStock()).isEqualTo(0);
        assertThat(productRepository.findById(product3.getId()).orElseThrow().getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 시나리오 5: 쿠폰 발급과 상품 구매가 동시에 발생")
    void concurrentCouponAndOrder() throws InterruptedException {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "할인 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            50,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        couponEventRepository.save(couponEvent);

        Product product = productRepository.save(
            new Product(null, "상품", "설명", 10000, 50)
        );

        // 50명 사용자 생성
        for (long i = 1; i <= 50; i++) {
            User user = userRepository.save(new User(i, "사용자" + i, 50000));
            CartItem cartItem = new CartItem(null, user.getId(), product.getId(), 1);
            cartRepository.save(cartItem);
        }

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(25);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        AtomicInteger couponSuccessCount = new AtomicInteger(0);
        AtomicInteger orderSuccessCount = new AtomicInteger(0);

        // when - 쿠폰 발급과 주문이 동시에 발생
        for (long i = 1; i <= threadCount; i++) {
            long userId = i;
            // 쿠폰 발급
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(1L, userId);
                    couponSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패 무시
                } finally {
                    latch.countDown();
                }
            });
            // 주문
            executorService.submit(() -> {
                try {
                    createOrderUseCase.execute(userId, null);
                    orderSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 모든 작업이 정상 완료되어야 함
        assertThat(couponSuccessCount.get()).isEqualTo(50);
        assertThat(orderSuccessCount.get()).isEqualTo(50);

        CouponEvent resultCoupon = couponEventRepository.findById(1L).orElseThrow();
        assertThat(resultCoupon.getIssuedQuantity()).isEqualTo(50);

        Product resultProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(resultProduct.getStock()).isEqualTo(0);
    }
}
