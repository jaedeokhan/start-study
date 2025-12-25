# 장애 대응 문서 (Incident Response Playbook)

## 📋 목차
1. 장애 시나리오 #2: Cache Stampede (캐시 스탬피드)
2. 장애 시나리오 #3: Redis 장애 (컨테이너 다운)
3. 장애 시나리오 #4: DB Connection Pool 고갈]
4. 공통 대응 절차

---

## 🚨 장애 시나리오 #2: Cache Stampede (캐시 스탬피드)

### 1. 장애 개요

| 항목 | 내용 |
|------|------|
| **발생 일시** | 2025-01-XX 10:00 (가상 시나리오) |
| **장애 유형** | 대규모 트래픽으로 인한 DB 과부하 |
| **심각도** | 🔴 Critical |
| **영향 범위** | 전체 API (캐시 의존 서비스) |
| **영향 사용자** | 전체 사용자 |
| **서비스 상태** | 🔴 Down (서비스 불가) |

#### 증상
```
[시스템 메트릭]
- 모든 API 응답 시간: 5초 이상
- Redis 연결 타임아웃 급증
- WAS CPU 사용률: 100%
- DB Connection Pool: 100% 사용 (고갈)
- 에러율: 80%

[사용자 증상]
- 모든 페이지 로딩 실패
- 타임아웃 에러 발생
```

### 2. 근본 원인: Cache Stampede

#### 2.1 발생 시나리오

```
[시간순 이벤트]

10:00:00 - Redis 캐시 TTL 만료 (5분)
         - 캐시 키: "productList:0:20"
         ↓
10:00:00 - 동시에 1,000개 요청 도착
         ↓
10:00:01 - 모든 요청이 캐시 미스 감지
         ↓
10:00:01 - 1,000개 요청이 동시에 DB 접속 시도
         ↓
10:00:02 - DB Connection Pool 고갈 (최대 100개)
         ↓
10:00:03 - 900개 요청 대기 → 타임아웃
         ↓
10:00:05 - 전체 서비스 마비
```

#### 2.2 Cache Stampede란?

```
[정상 상황]
요청 1 → 캐시 히트 → 즉시 응답
요청 2 → 캐시 히트 → 즉시 응답
요청 3 → 캐시 히트 → 즉시 응답

[Cache Stampede 상황]
캐시 만료
   ↓
요청 1 → 캐시 미스 → DB 조회 (695ms)
요청 2 → 캐시 미스 → DB 조회 (695ms)  ← 동시에 발생!
요청 3 → 캐시 미스 → DB 조회 (695ms)
...
요청 1000 → DB 과부하 → 타임아웃
```

### 3. 즉시 조치 (Emergency Response)

#### 3.1 긴급 복구 (5분 이내)

**🔴 Step 1: 수동 캐시 워밍 (Manual Cache Warming)**

```bash
# Redis CLI 접속
redis-cli

# 상품 목록 캐시 수동 삽입
SET "productList:0:20" '{"products":[...], "totalElements":100}'
EXPIRE "productList:0:20" 300  # TTL 5분

# 또는 Spring Boot Actuator를 통한 캐시 워밍
curl -X POST http://localhost:8080/actuator/caches/product-list/warm
```

---

**🟡 Step 2: DB Connection Pool 긴급 증가**

```yaml
# application.yml 수정 (재시작 필요)
spring:
  datasource:
    hikari:
      maximum-pool-size: 200  # 100 → 200으로 증가
      minimum-idle: 50
      connection-timeout: 30000
```

```bash
# 애플리케이션 재시작
systemctl restart ecommerce-api
```

---

**🟢 Step 3: Circuit Breaker 수동 Open**

```bash
# Resilience4j Circuit Breaker를 수동으로 Open
curl -X POST http://localhost:8080/actuator/circuitbreakers/productDB/open

# 일시적으로 DB 호출 차단, Fallback 응답 반환
```

### 4. 근본 해결 (Root Fix)

#### 4.1 분산 락을 이용한 Cache Stampede 방지

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

    @Cacheable(value = "productList", key = "#page + ':' + #size")
    public ProductListResponse getProducts(int page, int size) {
        String lockKey = "lock:productList:" + page + ":" + size;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 첫 번째 요청만 락 획득, 나머지는 대기
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);

            if (acquired) {
                log.info("🔒 Lock acquired, querying DB");
                // DB 조회
                Page<Product> products = productRepository.findAll(
                    PageRequest.of(page, size)
                );
                return ProductListResponse.from(products);
            } else {
                log.warn("⚠️ Failed to acquire lock, waiting for cache...");
                // 락 획득 실패 시 짧은 대기 후 캐시 재조회
                Thread.sleep(100);
                return getCachedProducts(page, size);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 Lock released");
            }
        }
    }
}
```

**동작 방식**:
1. 캐시 만료 시 첫 번째 요청만 락 획득
2. 첫 번째 요청이 DB 조회 후 캐시 저장
3. 나머지 요청들은 대기 후 캐시에서 조회

---

#### 4.2 백그라운드 캐시 갱신 (Proactive Refresh)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheRefreshScheduler {

    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 240000)  // 4분마다 (TTL 5분보다 짧게)
    public void refreshProductListCache() {
        log.info("🔄 Refreshing product list cache...");

        try {
            // 인기 페이지 미리 갱신 (0~9 페이지)
            for (int page = 0; page < 10; page++) {
                ProductListResponse response = productService
                    .getProductsWithoutCache(page, 20);  // 캐시 우회

                String cacheKey = "productList::" + page + ":20";
                redisTemplate.opsForValue().set(
                    cacheKey,
                    response,
                    5,
                    TimeUnit.MINUTES
                );

                log.info("✅ Cache refreshed for page {}", page);
            }
        } catch (Exception e) {
            log.error("❌ Failed to refresh cache", e);
            // 알림 발송 (Slack, PagerDuty 등)
        }
    }
}
```

**장점**:
- 캐시 만료 전에 미리 갱신
- 사용자는 항상 캐시 히트

---

#### 4.3 Soft Expiration (유연한 만료)

```java
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Soft Expiration: TTL + Grace Period
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(6))  // 실제 만료: 6분
            .prefixCacheNameWith("cache::")
            .serializeValuesWith(...);

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}

@Service
public class ProductService {

    @Cacheable(value = "productList", key = "#page + ':' + #size")
    public ProductListResponse getProducts(int page, int size) {
        // 논리적 만료: 5분
        // 실제 만료: 6분
        // → 5~6분 사이에는 "오래된" 캐시 반환 (Stale While Revalidate)

        ProductListResponse response = fetchFromDB(page, size);
        response.setCachedAt(LocalDateTime.now());  // 캐시 생성 시각 기록
        return response;
    }

    public ProductListResponse getProductsWithStaleCheck(int page, int size) {
        String cacheKey = "productList::" + page + ":" + size;
        ProductListResponse cached = (ProductListResponse)
            redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            Duration age = Duration.between(cached.getCachedAt(), LocalDateTime.now());

            if (age.toMinutes() > 5) {
                // 5분 이상 된 캐시 → 백그라운드에서 갱신
                CompletableFuture.runAsync(() -> refreshCache(page, size));
                log.info("⚠️ Serving stale cache, refreshing in background");
            }

            return cached;  // 오래된 캐시라도 반환
        }

        return fetchFromDB(page, size);
    }
}
```

---

#### 4.4 Circuit Breaker 적용

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    @CircuitBreaker(
        name = "productDB",
        fallbackMethod = "fallbackGetProducts"
    )
    @Cacheable(value = "productList", key = "#page + ':' + #size")
    public ProductListResponse getProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size));
    }

    // Fallback: 장애 시 빈 응답 또는 기본 데이터 반환
    public ProductListResponse fallbackGetProducts(
        int page, int size, Exception ex
    ) {
        log.warn("⚠️ Circuit breaker opened, returning fallback", ex);

        return ProductListResponse.builder()
            .products(Collections.emptyList())
            .message("일시적인 문제로 상품 목록을 불러올 수 없습니다.")
            .build();
    }
}
```

**Resilience4j 설정**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      productDB:
        failure-rate-threshold: 50  # 실패율 50% 시 Open
        slow-call-rate-threshold: 50  # 느린 요청 50% 시 Open
        slow-call-duration-threshold: 1000  # 1초 이상이 느린 요청
        wait-duration-in-open-state: 60000  # Open 상태 유지 시간 (1분)
        sliding-window-size: 10
```

### 5. 재발 방지

#### 5.1 캐시 워밍업 자동화

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCacheOnStartup() {
        log.info("🔥 Cache warming up on startup...");

        // 인기 페이지 미리 로드
        IntStream.range(0, 10).parallel().forEach(page -> {
            try {
                productService.getProducts(page, 20);
                log.info("✅ Warmed up page {}", page);
            } catch (Exception e) {
                log.error("❌ Failed to warm up page {}", page, e);
            }
        });

        log.info("🎉 Cache warming completed");
    }
}
```

#### 5.2 모니터링 및 알림

```yaml
# Prometheus Alert
- alert: HighCacheMissRate
  expr: |
    rate(cache_access_total{result="miss"}[5m])
    /
    rate(cache_access_total[5m])
    > 0.1  # 캐시 미스율 10% 초과
  for: 2m
  annotations:
    summary: "캐시 미스율 높음 (> 10%)"
    description: "Cache Stampede 가능성 있음"
```

---

<a name="시나리오3"></a>
## 🚨 장애 시나리오 #3: Redis 장애 (컨테이너 다운)

### 1. 장애 개요

| 항목 | 내용 |
|------|------|
| **발생 일시** | 2025-01-XX 15:00 (가상 시나리오) |
| **장애 유형** | 인프라 장애 (Redis 컨테이너 종료) |
| **심각도** | 🔴 Critical |
| **영향 범위** | 캐시 의존 API (상품 단건, 인기 상품, 상품 목록) |
| **영향 사용자** | 전체 사용자 |
| **서비스 상태** | 🔴 Down |

#### 증상
```
[에러 로그]
org.springframework.data.redis.RedisConnectionFailureException:
Unable to connect to Redis; Connection refused

[시스템 메트릭]
- Redis 연결 실패율: 100%
- 캐시 의존 API 응답 시간: 100배 증가 (0.66ms → 66ms)
- DB CPU 사용률: 90% (캐시 미스로 인한 DB 부하)
```

### 2. 근본 원인

```
[원인 분석]
Redis 컨테이너 메모리 부족 → OOM Killer → 컨테이너 강제 종료

[상세]
1. Redis 메모리 한계: 4GB
2. 캐시 데이터 누적: 5GB 도달
3. 메모리 초과 → OOM Killer 작동
4. 컨테이너 종료
```

### 3. 즉시 조치

#### 3.1 Redis 재시작 (2분)

```bash
# Docker 컨테이너 재시작
docker restart redis

# 상태 확인
docker ps | grep redis
docker logs redis

# Redis 연결 테스트
redis-cli ping
# 응답: PONG
```

#### 3.2 Fallback 로직 활성화

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public ProductListResponse getProducts(int page, int size) {
        try {
            // 1. Redis 캐시 조회 시도
            String cacheKey = "productList:" + page + ":" + size;
            ProductListResponse cached = (ProductListResponse)
                redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                return cached;
            }
        } catch (RedisConnectionException ex) {
            log.warn("⚠️ Redis unavailable, falling back to DB", ex);
            // Redis 장애 시 DB 직접 조회
        }

        // 2. Fallback: DB 직접 조회
        Page<Product> products = productRepository.findAll(
            PageRequest.of(page, size)
        );

        return ProductListResponse.from(products);
    }
}
```

### 4. 근본 해결

#### 4.1 Redis 메모리 증설

```yaml
# docker-compose.yml
services:
  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    deploy:
      resources:
        limits:
          memory: 8G  # 4G → 8G로 증설
        reservations:
          memory: 4G
    command: redis-server --maxmemory 6gb --maxmemory-policy allkeys-lru
```

#### 4.2 Eviction 정책 설정

```conf
# redis.conf
maxmemory 6gb
maxmemory-policy allkeys-lru  # LRU 방식으로 자동 삭제

# 정책 옵션:
# - noeviction: 메모리 꽉 차면 에러 (기본값)
# - allkeys-lru: 모든 키 중 LRU로 삭제
# - volatile-lru: TTL 있는 키 중 LRU로 삭제
# - allkeys-random: 모든 키 중 랜덤 삭제
# - volatile-ttl: TTL이 짧은 키부터 삭제
```

#### 4.3 Redis Sentinel (High Availability)

```yaml
# docker-compose.yml
version: '3.8'

services:
  redis-master:
    image: redis:7
    command: redis-server --port 6379
    ports:
      - "6379:6379"

  redis-slave-1:
    image: redis:7
    command: redis-server --port 6380 --replicaof redis-master 6379
    depends_on:
      - redis-master

  redis-slave-2:
    image: redis:7
    command: redis-server --port 6381 --replicaof redis-master 6379
    depends_on:
      - redis-master

  redis-sentinel-1:
    image: redis:7
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel.conf:/etc/redis/sentinel.conf

  redis-sentinel-2:
    image: redis:7
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel.conf:/etc/redis/sentinel.conf

  redis-sentinel-3:
    image: redis:7
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel.conf:/etc/redis/sentinel.conf
```

**sentinel.conf**:
```conf
sentinel monitor mymaster redis-master 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 10000
```

---

<a name="시나리오4"></a>
## 🚨 장애 시나리오 #4: DB Connection Pool 고갈

### 1. 장애 개요

| 항목 | 내용 |
|------|------|
| **발생 일시** | 2025-01-XX 16:00 (가상 시나리오) |
| **장애 유형** | 리소스 고갈 |
| **심각도** | 🔴 Critical |
| **영향 범위** | 모든 DB 조회 API |
| **서비스 상태** | 🔴 Down |

#### 증상
```
[에러 로그]
HikariPool-1 - Connection is not available,
request timed out after 30000ms

[시스템 메트릭]
- DB Connection Pool 사용률: 100% (100/100)
- 대기 중인 요청: 500개
- 평균 응답 시간: 30초 (타임아웃)
```

### 2. 근본 원인

```
캐시 장애 → 모든 요청이 DB 직접 조회 → Connection Pool 고갈
```

### 3. 즉시 조치

```yaml
# application.yml 긴급 수정
spring:
  datasource:
    hikari:
      maximum-pool-size: 200  # 100 → 200
      connection-timeout: 10000  # 10초
```

### 4. 근본 해결

```java
@Configuration
public class HikariConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/ecommerce");
        config.setUsername("admin");
        config.setPassword("password");

        // Connection Pool 설정
        config.setMaximumPoolSize(50);  // 최대 50개
        config.setMinimumIdle(10);      // 최소 10개 유지
        config.setConnectionTimeout(30000);  // 30초
        config.setIdleTimeout(600000);  // 10분
        config.setMaxLifetime(1800000);  // 30분

        // 연결 유효성 검사
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }
}
```

---

<a name="공통절차"></a>
## 📋 공통 대응 절차

### 1. 장애 감지 → 알림 (1분 이내)

```yaml
# Prometheus Alert Rules
groups:
- name: critical_alerts
  rules:
  - alert: ServiceDown
    expr: up{job="ecommerce-api"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "서비스 다운"

  - alert: HighErrorRate
    expr: |
      rate(http_requests_total{status=~"5.."}[5m])
      /
      rate(http_requests_total[5m])
      > 0.05  # 에러율 5% 초과
    for: 2m
    labels:
      severity: critical
```

### 2. 초기 대응 (5분 이내)

#### Checklist
- [ ] 장애 상황 확인 (모니터링 대시보드)
- [ ] 관련팀 긴급 소집 (Slack, PagerDuty)
- [ ] 장애 범위 파악 (영향받는 API, 사용자 수)
- [ ] 임시 조치 실행 (재시작, Fallback 등)

### 3. 근본 원인 분석 (1시간 이내)

#### 분석 도구
1. **로그 분석**: ELK Stack
2. **APM**: Scouter
3. **DB 쿼리**: pgAdmin, EXPLAIN ANALYZE
4. **인프라**: Docker stats, htop

### 4. 근본 해결 (1-7일)

1. 코드 수정 (캐시, 인덱스 등)
2. 테스트 (단위 + 통합 + 부하)
3. 배포 (Canary/Blue-Green)
4. 모니터링 강화

### 5. 사후 리뷰 (1주 이내)

**Post-Mortem 문서 작성**:
- 장애 개요
- Timeline
- 근본 원인
- 조치 사항
- 재발 방지 대책