package com.ecommerce.config;

import com.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.User;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.infrastructure.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 초기 데이터 설정
 * - Application 시작 시 테스트용 데이터 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponEventRepository couponEventRepository;
    private final CartRepository cartRepository;
    private final CreateOrderUseCase createOrderUseCase;  // UseCase 주입

    @PostConstruct
    public void init() {
        log.info("=== 초기 데이터 설정 시작 ===");

        initProducts();
        initUsers();
        initCouponEvents();
        initCartItems();



        log.info("=== 초기 데이터 설정 완료 ===");
    }

    private void initProducts() {
        log.info("상품 데이터 초기화 중...");

        // 전자제품
        productRepository.save(new Product(null, "노트북", "고성능 노트북, 16GB RAM, 512GB SSD", 1500000L, 50));
        productRepository.save(new Product(null, "마우스", "무선 마우스", 30000L, 100));
        productRepository.save(new Product(null, "키보드", "기계식 키보드", 120000L, 80));
        productRepository.save(new Product(null, "모니터", "27인치 QHD 모니터", 350000L, 30));
        productRepository.save(new Product(null, "헤드셋", "게이밍 헤드셋", 80000L, 60));

        // 생활용품
        productRepository.save(new Product(null, "텀블러", "보온 텀블러 500ml", 25000L, 200));
        productRepository.save(new Product(null, "우산", "자동 우산", 15000L, 150));
        productRepository.save(new Product(null, "가방", "노트북 백팩", 89000L, 70));

        log.info("상품 데이터 초기화 완료: 8개 상품");
    }

    private void initUsers() {
        log.info("사용자 데이터 초기화 중...");

        userRepository.save(new User(null, "홍길동", 5000000L));
        userRepository.save(new User(null, "김철수", 3000000L));
        userRepository.save(new User(null, "이영희", 2000000L));

        log.info("사용자 데이터 초기화 완료: 3명");
    }

    private void initCouponEvents() {
        log.info("쿠폰 이벤트 데이터 초기화 중...");

        LocalDateTime now = LocalDateTime.now();

        // AMOUNT 타입 쿠폰
        couponEventRepository.save(new CouponEvent(
            null,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            10000L,
            1000,
            now.minusDays(1),
            now.plusDays(30)
        ));

        couponEventRepository.save(new CouponEvent(
            null,
            "겨울 시즌 할인",
            DiscountType.AMOUNT,
            50000L,
            500,
            now.minusDays(5),
            now.plusDays(60)
        ));

        // RATE 타입 쿠폰
        couponEventRepository.save(new CouponEvent(
            null,
            "첫 구매 10% 할인",
            DiscountType.RATE,
            10,
            50000,
            500,
            now.minusDays(3),
            now.plusDays(45)
        ));

        couponEventRepository.save(new CouponEvent(
            null,
            "VIP 20% 할인",
            DiscountType.RATE,
            20,
            100000,
            100,
            now.minusDays(7),
            now.plusDays(90)
        ));

        log.info("쿠폰 이벤트 데이터 초기화 완료: 4개 이벤트");
    }

    private void initCartItems() {
        log.info("장바구니 데이터 초기화 중...");

        try {
            List<User> users = userRepository.findAll();
            List<Product> products = productRepository.findAll();

            if (users.isEmpty() || products.isEmpty()) {
                log.warn("사용자 또는 상품 데이터가 없어 장바구니 초기화를 건너뜁니다.");
                return;
            }

            int cartItemCount = 0;

            // 사용자 1 (홍길동) - 노트북, 마우스, 키보드
            if (users.size() > 0 && products.size() >= 3) {
                Long userId1 = users.get(0).getId();
                cartRepository.save(new CartItem(null, userId1, products.get(0).getId(), 1));
                cartRepository.save(new CartItem(null, userId1, products.get(1).getId(), 2));
                cartRepository.save(new CartItem(null, userId1, products.get(2).getId(), 1));
                cartItemCount += 3;
            }

            // 사용자 2 (김철수) - 모니터, 헤드셋
            if (users.size() > 1 && products.size() >= 5) {
                Long userId2 = users.get(1).getId();
                cartRepository.save(new CartItem(null, userId2, products.get(3).getId(), 1));
                cartRepository.save(new CartItem(null, userId2, products.get(4).getId(), 1));
                cartItemCount += 2;
            }

            // 사용자 3 (이영희) - 텀블러, 우산, 가방
            if (users.size() > 2 && products.size() >= 8) {
                Long userId3 = users.get(2).getId();
                cartRepository.save(new CartItem(null, userId3, products.get(5).getId(), 3));
                cartRepository.save(new CartItem(null, userId3, products.get(6).getId(), 2));
                cartRepository.save(new CartItem(null, userId3, products.get(7).getId(), 1));
                cartItemCount += 3;
            }

            log.info("장바구니 데이터 초기화 완료: {}개 아이템", cartItemCount);

            createOrderUseCase.execute(1L, null);
            createOrderUseCase.execute(2L, null);
            createOrderUseCase.execute(3L, null);

        } catch (Exception e) {
            log.error("장바구니 데이터 초기화 실패: {}", e.getMessage(), e);
        }
    }
}
