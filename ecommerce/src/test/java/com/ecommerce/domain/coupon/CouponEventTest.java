package com.ecommerce.domain.coupon;

import com.ecommerce.domain.coupon.exception.CouponSoldOutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CouponEvent 도메인 엔티티 테스트")
class CouponEventTest {

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 수량 있음")
    void canIssueWhenQuantityAvailable() {
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

        // when & then
        assertThat(couponEvent.canIssue()).isTrue();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 수량 소진")
    void canIssueWhenQuantityExhausted() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            2,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );

        // when
        couponEvent.issue();
        couponEvent.issue();

        // then
        assertThat(couponEvent.canIssue()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueSuccess() {
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

        // when
        couponEvent.issue();

        // then
        assertThat(couponEvent.getIssuedQuantity()).isEqualTo(1);
        assertThat(couponEvent.getRemainingQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("쿠폰 수량 소진 시 발급 실패")
    void issueFailWhenSoldOut() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            1,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        );

        // when
        couponEvent.issue();

        // then
        assertThatThrownBy(() -> couponEvent.issue())
            .isInstanceOf(CouponSoldOutException.class)
            .hasMessageContaining("쿠폰이 모두 소진");
    }

    @Test
    @DisplayName("이벤트 진행 중 여부 확인 - 진행 중")
    void isActiveWhenInPeriod() {
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

        // when & then
        assertThat(couponEvent.isActive()).isTrue();
    }

    @Test
    @DisplayName("이벤트 진행 중 여부 확인 - 종료됨")
    void isActiveWhenExpired() {
        // given
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(3)
        );

        // when & then
        assertThat(couponEvent.isActive()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 기간 내 & 수량 있음")
    void isAvailableWhenPeriodAndQuantityOk() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            now.minusDays(1),
            now.plusDays(7)
        );

        // when & then
        assertThat(couponEvent.isAvailable(now)).isTrue();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 기간 만료")
    void isAvailableWhenExpired() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CouponEvent couponEvent = new CouponEvent(
            1L,
            "신규 가입 쿠폰",
            DiscountType.AMOUNT,
            5000L,
            100,
            now.minusDays(10),
            now.minusDays(3)
        );

        // when & then
        assertThat(couponEvent.isAvailable(now)).isFalse();
    }

    @Test
    @DisplayName("남은 수량 조회")
    void getRemainingQuantity() {
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

        // when
        couponEvent.issue();
        couponEvent.issue();
        couponEvent.issue();

        // then
        assertThat(couponEvent.getRemainingQuantity()).isEqualTo(97);
    }
}
