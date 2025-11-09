package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.exception.CouponAlreadyUsedException;
import com.ecommerce.domain.coupon.exception.CouponExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserCoupon 도메인 엔티티 테스트")
class UserCouponTest {

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCouponSuccess() {
        // given
        UserCoupon userCoupon = new UserCoupon(
            1L, 1L, 1L,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );

        // when
        userCoupon.use();

        // then
        assertThat(userCoupon.isUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 사용 시 예외 발생")
    void useCouponAlreadyUsed() {
        // given
        UserCoupon userCoupon = new UserCoupon(
            1L, 1L, 1L,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );
        userCoupon.use();

        // when & then
        assertThatThrownBy(() -> userCoupon.use())
            .isInstanceOf(CouponAlreadyUsedException.class);
    }

    @Test
    @DisplayName("만료된 쿠폰 사용 시 예외 발생")
    void useCouponExpired() {
        // given
        UserCoupon userCoupon = new UserCoupon(
            1L, 1L, 1L,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(3)
        );

        // when & then
        assertThatThrownBy(() -> userCoupon.use())
            .isInstanceOf(CouponExpiredException.class);
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부 확인")
    void canUseCoupon() {
        // given
        UserCoupon userCoupon = new UserCoupon(
            1L, 1L, 1L,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );

        // when & then
        assertThat(userCoupon.canUse()).isTrue();
    }

    @Test
    @DisplayName("만료된 쿠폰은 사용 불가")
    void cannotUseExpiredCoupon() {
        // given
        UserCoupon userCoupon = new UserCoupon(
            1L, 1L, 1L,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(3)
        );

        // when & then
        assertThat(userCoupon.canUse()).isFalse();
        assertThat(userCoupon.isExpired()).isTrue();
    }
}
