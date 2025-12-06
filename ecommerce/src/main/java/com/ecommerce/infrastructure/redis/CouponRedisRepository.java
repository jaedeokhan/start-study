package com.ecommerce.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CouponRedisRepository {

    private static final String COUPON_QUEUE_PREFIX = "ecommerce:async:coupon:queue:";
    private static final String COUPON_ISSUED_PREFIX = "ecommerce:async:coupon:issued:";  // 중복 체크용
    private static final String COUPON_STOCK_PREFIX = "ecommerce:async:coupon:stock:";
    private static final Duration KEY_TTL = Duration.ofDays(10);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 재고 초기화
     */
    public void initializeCouponStock(Long couponEventId, int totalQuantity) {
        String stockKey = COUPON_STOCK_PREFIX + couponEventId;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(totalQuantity), KEY_TTL);
        log.info("쿠폰 재고 초기화 - eventId: {}, quantity: {}", couponEventId, totalQuantity);
    }

    /**
     * 쿠폰 발급 요청
     * 1. ZADD NX로 중복 체크 + 발급 기록
     * 2. DECR로 재고 차감
     * 3. RPUSH로 큐에 추가
     */
    public boolean tryIssueCoupon(Long couponEventId, Long userId) {
        String queueKey = COUPON_QUEUE_PREFIX + couponEventId;
        String issuedKey = COUPON_ISSUED_PREFIX + couponEventId;
        String stockKey = COUPON_STOCK_PREFIX + couponEventId;

        // 1. ZADD NX로 중복 체크 (원자적)
        long timestamp = System.currentTimeMillis();
        Boolean added = redisTemplate.opsForZSet()
                .addIfAbsent(issuedKey, String.valueOf(userId), timestamp);

        if (Boolean.FALSE.equals(added)) {
            log.debug("중복 발급 - eventId: {}, userId: {}", couponEventId, userId);
            return false;
        }

        // 2. 재고 차감
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

        if (remainingStock == null || remainingStock < 0) {
            // 재고 부족 - ZSET에서 제거 (롤백)
            redisTemplate.opsForZSet().remove(issuedKey, String.valueOf(userId));
            redisTemplate.opsForValue().increment(stockKey);
            log.debug("재고 부족 - eventId: {}, userId: {}", couponEventId, userId);
            return false;
        }

        // 3. Queue에 userId 추가
        redisTemplate.opsForList().rightPush(queueKey, String.valueOf(userId));

        // TTL 설정
        redisTemplate.expire(issuedKey, KEY_TTL);
        redisTemplate.expire(queueKey, KEY_TTL);

        log.info("큐 추가 성공 - eventId: {}, userId: {}, queueSize: {}",
                couponEventId, userId, getQueueSize(couponEventId));
        return true;
    }

    /**
     * 대기열에서 Bulk로 userId 꺼내기 (배치 처리용)
     */
    public List<Long> popFromQueue(Long couponEventId, int count) {
        String queueKey = COUPON_QUEUE_PREFIX + couponEventId;
        List<Long> userIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String userIdStr = redisTemplate.opsForList().leftPop(queueKey);
            if (userIdStr == null) {
                break;
            }

            try {
                Long userId = Long.parseLong(userIdStr);
                userIds.add(userId);
            } catch (NumberFormatException e) {
                log.error("userId 파싱 실패 - value: {}", userIdStr, e);
            }
        }

        if (!userIds.isEmpty()) {
            log.info("큐에서 꺼내기 완료 - eventId: {}, count: {}", couponEventId, userIds.size());
        }

        return userIds;
    }

    /**
     * 큐 크기 조회
     */
    public long getQueueSize(Long couponEventId) {
        String queueKey = COUPON_QUEUE_PREFIX + couponEventId;
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * 발급 여부 확인 (ZSET ZSCORE)
     */
    public boolean isAlreadyIssued(Long couponEventId, Long userId) {
        String issuedKey = COUPON_ISSUED_PREFIX + couponEventId;
        Double score = redisTemplate.opsForZSet().score(issuedKey, String.valueOf(userId));
        return score != null;
    }

    /**
     * 남은 재고 조회
     */
    public int getRemainingStock(Long couponEventId) {
        String stockKey = COUPON_STOCK_PREFIX + couponEventId;
        String stockValue = redisTemplate.opsForValue().get(stockKey);

        if (stockValue == null) {
            return 0;
        }

        try {
            int stock = Integer.parseInt(stockValue);
            return Math.max(0, stock);
        } catch (NumberFormatException e) {
            log.error("재고 파싱 실패 - stockKey: {}, value: {}", stockKey, stockValue);
            return 0;
        }
    }
}