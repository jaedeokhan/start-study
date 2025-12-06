package com.ecommerce.infrastructure.scheduler;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.infrastructure.redis.CouponRedisRepository;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponBatchScheduler {

    private final CouponRedisRepository couponRedisRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;

    private static final int BATCH_SIZE = 100;

    /**
     * 10초마다 큐에서 꺼내서 DB에 저장
     */
    @Scheduled(fixedDelay = 10000)
    public void processCouponQueue() {
        List<CouponEvent> activeEvents = couponEventRepository.findActiveEvents(LocalDateTime.now());

        for (CouponEvent event : activeEvents) {
            processEventQueue(event);
        }
    }

    private void processEventQueue(CouponEvent event) {
        try {
            long queueSize = couponRedisRepository.getQueueSize(event.getId());
            if (queueSize == 0) {
                return;
            }

            log.info("큐 처리 시작 - eventId: {}, queueSize: {}", event.getId(), queueSize);

            // 큐에서 userId 리스트 꺼내기
            List<Long> userIds = couponRedisRepository.popFromQueue(event.getId(), BATCH_SIZE);

            if (userIds.isEmpty()) {
                return;
            }

            // UserCoupon 생성
            List<UserCoupon> userCoupons = userIds.stream()
                    .map(userId -> new UserCoupon(
                            null,
                            userId,
                            event.getId(),
                            event.getStartDate(),
                            event.getEndDate()
                    ))
                    .collect(Collectors.toList());

            // Bulk Insert
            userCouponRepository.saveAll(userCoupons);

            log.info("큐 처리 완료 - eventId: {}, count: {}", event.getId(), userIds.size());

        } catch (Exception e) {
            log.error("큐 처리 실패 - eventId: {}", event.getId(), e);
        }
    }
}