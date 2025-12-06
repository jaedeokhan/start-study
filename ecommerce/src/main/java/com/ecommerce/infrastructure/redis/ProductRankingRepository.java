package com.ecommerce.infrastructure.redis;

import com.ecommerce.presentation.dto.product.ProductRankingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductRankingRepository {

    private static final String RANKING_KEY_PREFIX = "ecommerce:cache:ranking:daily:";
    private static final String RANKING_KEY_TEMP_PREFIX = "ecommerce:cache:ranking:daily:temp:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration TTL = Duration.ofDays(3);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 오늘 날짜 키에 상품별 판매량 증가 (Pipeline 사용)
     *
     * @param productQuantities Map<productId, quantity>
     */
    public void incrementTodayRanking(Map<Long, Integer> productQuantities) {
        if (productQuantities.isEmpty()) {
            return;
        }

        String todayKey = getTodayKey();

        // Pipeline으로 배치 처리
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

            productQuantities.forEach((productId, quantity) -> {
                zSetOps.incrementScore(todayKey, String.valueOf(productId), quantity);
            });

            // TTL 설정 (3일 후 자동 삭제)
            redisTemplate.expire(todayKey, TTL);

            return null;
        });

        log.info("일간 랭킹 업데이트 완료 - key: {}, 상품 수: {}", todayKey, productQuantities.size());
    }

    /**
     * 최근 3일간 많이 팔린 상품 Top 5 조회
     *
     * @return List<ProductRankingDto> (productId, totalQuantity, rank)
     */
    public List<ProductRankingDto> getTop5Last3Days() {
        LocalDate today = LocalDate.now();

        // 최근 3일 키 생성
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            keys.add(getKeyForDate(today.minusDays(i)));
        }

        // 임시 합산 키 (UUID로 고유하게)
        String tempKey = RANKING_KEY_TEMP_PREFIX + UUID.randomUUID();

        try {
            // ZUNIONSTORE: 3일치 데이터 합산
            if (keys.size() == 1) {
                redisTemplate.opsForZSet().unionAndStore(keys.get(0),
                        Collections.emptyList(), tempKey);
            } else {
                redisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1,
                        keys.size()), tempKey);
            }

            // TTL 설정 (1분 후 자동 삭제)
            redisTemplate.expire(tempKey, Duration.ofMinutes(1));

            // ZREVRANGE: Top 5 조회 (내림차순)
            Set<ZSetOperations.TypedTuple<String>> results =
                    redisTemplate.opsForZSet().reverseRangeWithScores(tempKey, 0, 4);

            if (results == null || results.isEmpty()) {
                log.info("최근 3일간 랭킹 데이터 없음");
                return Collections.emptyList();
            }

            // DTO 변환
            List<ProductRankingDto> rankings = new ArrayList<>();
            long rank = 1;

            for (ZSetOperations.TypedTuple<String> tuple : results) {
                Long productId = Long.valueOf(tuple.getValue());
                Integer quantity = tuple.getScore().intValue();

                rankings.add(new ProductRankingDto(productId, quantity, rank++));
            }

            log.info("최근 3일 Top 5 조회 완료 - {} 건", rankings.size());
            return rankings;

        } finally {
            // 임시 키 삭제
            redisTemplate.delete(tempKey);
        }
    }

    /**
     * 오늘 날짜의 Redis Key 생성
     * 예: "ecommerce:ranking:daily:20250604"
     */
    private String getTodayKey() {
        return getKeyForDate(LocalDate.now());
    }

    /**
     * 특정 날짜의 Redis Key 생성
     */
    private String getKeyForDate(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}
