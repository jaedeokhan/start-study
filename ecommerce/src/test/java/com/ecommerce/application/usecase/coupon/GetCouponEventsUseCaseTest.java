package com.ecommerce.application.usecase.coupon;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.infrastructure.memory.InMemoryCouponEventRepository;
import com.ecommerce.presentation.dto.coupon.CouponEventListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GetCouponEventsUseCase 테스트")
class GetCouponEventsUseCaseTest {

    private GetCouponEventsUseCase getCouponEventsUseCase;
    private InMemoryCouponEventRepository couponEventRepository;

    @BeforeEach
    void setUp() {
        couponEventRepository = new InMemoryCouponEventRepository();
        getCouponEventsUseCase = new GetCouponEventsUseCase(couponEventRepository);
    }

    @Test
    @DisplayName("쿠폰 이벤트 목록 조회")
    void getCouponEvents() {
        // given
        couponEventRepository.save(new CouponEvent(
            1L, "쿠폰1", DiscountType.AMOUNT, 5000L, 100,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)
        ));
        couponEventRepository.save(new CouponEvent(
            2L, "쿠폰2", DiscountType.AMOUNT, 3000L, 50,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)
        ));

        // when
        CouponEventListResponse response = getCouponEventsUseCase.execute();

        // then
        assertThat(response.getEvents()).hasSize(2);
    }
}
