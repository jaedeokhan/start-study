# 📊 인기 상품 랭킹 시스템 성능 개선 보고서

## 1. Redis Sorted Set 기반 랭킹 시스템

### 개요
최근 3일간 인기 상품 Top 5 조회를 위해 Redis Sorted Set을 활용한 실시간 랭킹 시스템 구현

### 핵심 기술

#### Redis 자료구조
```
Key: ecommerce:ranking:daily:{yyyyMMdd}
Type: Sorted Set (ZSET)
Member: productId
Score: 판매 수량
TTL: 3일
```

#### 주요 명령어
| 명령어 | 용도 | 시점 |
|--------|------|------|
| `ZINCRBY` | 상품별 판매량 증가 | 주문 완료 시 (Pipeline) |
| `ZUNIONSTORE` | 3일치 데이터 합산 | Top 5 조회 시 |
| `ZREVRANGE` | Top 5 추출 | 합산 후 |

#### 주문 시 랭킹 업데이트 흐름
```
주문 생성 → DB 트랜잭션 커밋 → afterCommit() 콜백
  ↓
Pipeline으로 일괄 업데이트
  - ZINCRBY daily:20250604 {productId} {quantity}
  - EXPIRE daily:20250604 259200 (3일)
```

#### 조회 시 랭킹 계산 흐름
```
1. 최근 3일 키 생성
   - daily:20250604
   - daily:20250603
   - daily:20250602

2. ZUNIONSTORE (3일 합산)
   - 임시 키에 합산 결과 저장
   - TTL 1분 (자동 삭제)

3. ZREVRANGE 0 4 WITHSCORES
   - 점수 높은 순 Top 5 조회

4. DB에서 상품 정보 조회
   - productId로 상품명, 가격 등 조회
```

### 장점
- ✅ **실시간 업데이트**: 주문 즉시 랭킹 반영
- ✅ **O(log N) 성능**: Sorted Set의 효율적인 정렬
- ✅ **Pipeline 최적화**: 다중 명령어 일괄 처리
- ✅ **자동 정렬**: Score 기반 자동 순위 관리
- ✅ **TTL 관리**: 오래된 데이터 자동 삭제

---

## 2. Caffeine 로컬 캐시

### 개요
Redis 조회 부하를 최소화하기 위해 애플리케이션 메모리에 Caffeine 로컬 캐시 적용

### 핵심 설정

```java
@Primary
@Bean
public CacheManager caffeineCacheManager() {
    CaffeineCache popularProductsCache = new CaffeineCache(
        "product:popular",
        Caffeine.newBuilder()
            .maximumSize(1)                          // Top 5 결과 1개
            .expireAfterWrite(30, TimeUnit.SECONDS)  // TTL 30초
            .recordStats()                           // 통계 수집
            .build()
    );
    return new SimpleCacheManager(List.of(popularProductsCache));
}
```

### 장점
- ✅ **초고속 응답**: 0.1ms 미만 (메모리 직접 접근)
- ✅ **Redis 부하 감소**: 99.97% 감소 (30초마다 1회만 조회)
- ✅ **TTL 지원**: 30초 자동 만료로 준실시간 데이터
- ✅ **메모리 안전**: maximumSize로 메모리 누수 방지
- ✅ **모니터링**: recordStats()로 히트율 추적 가능

---

## 3. 성능 측정 결과

### 동시 요청 1,000건

| 지표 | No Cache | Redis Cache | Local Cache + Redis ZSet | 개선율 |
|-----|----------|-------------|-------------------------|-------|
| **총 처리 시간** | 3,347ms | 1,342ms | **521ms** | **84.4% ↓** |
| **평균 응답시간** | 33.076ms | 12.941ms | **3.574ms** | **89.2% ↓** |
| **최소 응답시간** | 12ms | 3ms | **0ms** | **100% ↓** |
| **최대 응답시간** | 163ms | 240ms | 257ms | - |
| **TPS** | 298.78 | 745.16 | **1,919.39** | **542.3% ↑** |

### 동시 요청 10,000건

| 지표 | No Cache | Redis Cache | Local Cache + Redis ZSet | 개선율 |
|-----|----------|-------------|-------------------------|-------|
| **총 처리 시간** | 25,156ms | 4,983ms | **2,948ms** | **88.3% ↓** |
| **평균 응답시간** | 250.49ms | 48.55ms | **21.80ms** | **91.3% ↓** |
| **최소 응답시간** | 7ms | 6ms | **0ms** | **100% ↓** |
| **최대 응답시간** | 1,337ms | 2,459ms | 2,928ms | - |
| **TPS** | 397.52 | 2,006.82 | **3,392.13** | **753.5% ↑** |

### 성능 개선 비교 차트

```
응답 시간 개선 (10,000건 기준)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 250.49ms  (No Cache)
━━━━━━━━━━━━━━━━━━━━━ 48.55ms  (Redis Cache)
━━━━━━━━━ 21.80ms  (Local Cache + Redis ZSet) ⭐

TPS 개선 (10,000건 기준)
━━━━━━━━━━━━━━━━ 397.52  (No Cache)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 2,006.82  (Redis Cache)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 3,392.13  (Local Cache + Redis ZSet) ⭐
```

---

## 4. 핵심 성과 요약

### Before (No Cache)
```
조회 방식: DB 집계 쿼리 (JOIN + GROUP BY + ORDER BY)
문제점:
- 매 요청마다 DB 부하
- 복잡한 집계 연산
- 느린 응답 속도 (평균 250ms)
```

### After v1 (Redis Cache)
```
조회 방식: DB 집계 → Redis 캐시 (10분 TTL)
개선점:
- 캐시 히트 시 빠른 응답
- DB 부하 감소

문제점:
- 최대 10분 지연
- Redis 네트워크 왕복
- 첫 요청은 여전히 느림
```

### After v2 (Local Cache + Redis Sorted Set) ⭐
```
조회 방식: Caffeine (30초) → Redis Sorted Set → DB
개선점:
✅ 초고속 응답 (0.1ms)
✅ 실시간 랭킹 (30초 갱신)
✅ Redis 부하 99.97% 감소
✅ TPS 7.5배 향상
✅ Pipeline 최적화
```

---

## 6. 결론

### 핵심 개선 사항
1. **Redis Sorted Set**: DB 집계를 Redis로 이관 → 실시간 랭킹
2. **Caffeine 로컬 캐시**: Redis 조회를 메모리 캐시로 → 초고속 응답
3. **Pipeline 최적화**: 다중 Redis 명령어 일괄 처리 → 네트워크 비용 감소

### 최종 성과
- 🚀 **응답 속도**: 250ms → 21ms (91% 개선)
- 🚀 **처리량(TPS)**: 397 → 3,392 (753% 향상)
- 🚀 **Redis 부하**: 99.97% 감소
- 🚀 **실시간성**: 10분 → 30초 (20배 빠른 갱신)