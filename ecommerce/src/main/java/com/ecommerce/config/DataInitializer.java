package com.ecommerce.config;

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

/**
 * 초기 데이터 설정
 * - Application 시작 시 테스트용 데이터 생성
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponEventRepository couponEventRepository;

    @PostConstruct
    public void init() {
        log.info("=== 초기 데이터 설정 시작 ===");

        initProducts();
        initUsers();
        initCouponEvents();

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
}
