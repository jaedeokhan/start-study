package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import com.ecommerce.infrastructure.memory.InMemoryCouponEventRepository;
import com.ecommerce.infrastructure.memory.InMemoryUserCouponRepository;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("IssueCouponUseCase 테스트")
class IssueCouponUseCaseTest {

    private IssueCouponUseCase issueCouponUseCase;
    private InMemoryCouponEventRepository couponEventRepository;
    private InMemoryUserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        couponEventRepository = new InMemoryCouponEventRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        issueCouponUseCase = new IssueCouponUseCase(couponEventRepository, userCouponRepository);
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCouponSuccess() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        couponEventRepository.save(couponEvent);

        // when
        IssueCouponResponse response = issueCouponUseCase.execute(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCouponName()).isEqualTo("신규 가입 쿠폰");
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 이벤트 발급 시 예외 발생")
    void issueCouponNotFound() {
        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(999L, 1L))
            .isInstanceOf(CouponEventNotFoundException.class);
    }

    @Test
    @DisplayName("기간이 만료된 쿠폰 발급 시 예외 발생")
    void issueCouponExpired() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "만료된 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(3)
        );
        couponEventRepository.save(couponEvent);

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(1L, 1L))
            .isInstanceOf(CouponExpiredException.class);
    }

    @Test
    @DisplayName("중복 발급 시 예외 발생")
    void issueCouponDuplicate() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        couponEventRepository.save(couponEvent);

        // 첫 번째 발급
        issueCouponUseCase.execute(1L, 1L);

        // when & then - 같은 사용자가 같은 쿠폰 재발급 시도
        assertThatThrownBy(() -> issueCouponUseCase.execute(1L, 1L))
            .isInstanceOf(CouponAlreadyIssuedException.class);
    }

    @Test
    @DisplayName("서로 다른 사용자는 같은 쿠폰을 발급받을 수 있음")
    void issueCouponDifferentUsers() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        couponEventRepository.save(couponEvent);

        // when
        IssueCouponResponse response1 = issueCouponUseCase.execute(1L, 1L);
        IssueCouponResponse response2 = issueCouponUseCase.execute(1L, 2L);

        // then
        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
        assertThat(response1.getId()).isNotEqualTo(response2.getId());
    }
}
