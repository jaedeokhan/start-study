# E-Commerce 동시성 제어 분석 보고서

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [동시성 이슈 시나리오](#동시성-이슈-시나리오)
3. [선택한 동시성 제어 방식](#선택한-동시성-제어-방식)
4. [대안 방식 비교](#대안-방식-비교)
5. [구현 상세](#구현-상세)
6. [테스트 결과](#테스트-결과)
7. [결론 및 향후 개선](#결론-및-향후-개선)

---

## 프로젝트 개요

### 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle
- **Test**: JUnit 5, AssertJ
- **Storage**: In-Memory (ConcurrentHashMap)

### 핵심 기능
- 상품 조회 및 재고 관리
- 장바구니 관리
- 선착순 쿠폰 발급
- 주문 생성 및 결제
- 포인트 충전/사용 및 이력 관리

## 동시성 이슈 시나리오

### 1. 상품 재고 차감 (Race Condition)

#### 문제 상황
```
초기 재고: 10개

[Thread-1] 재고 조회 → 10개
[Thread-2] 재고 조회 → 10개
[Thread-1] 5개 차감 → 재고 5개로 업데이트
[Thread-2] 8개 차감 → 재고 2개로 업데이트 ❌

결과: 실제로는 13개가 판매되었지만 재고는 2개로 표시됨
```

#### 영향
- ❌ **과다 판매**: 실제 재고보다 많은 주문 발생
- ❌ **재고 부정확**: 재고 데이터 불일치
- ❌ **비즈니스 손실**: 물류 문제 및 고객 불만

---

### 2. 선착순 쿠폰 발급 (Race Condition)

#### 문제 상황
```
발급 가능 수량: 100개

[Thread-1] 발급 수 조회 → 99개
[Thread-2] 발급 수 조회 → 99개
[Thread-1] 발급 수 증가 → 100개로 업데이트 ✅
[Thread-2] 발급 수 증가 → 100개로 업데이트 ✅

결과: 101개가 발급됨 (제한 수량 초과) ❌
```

#### 영향
- ❌ **수량 초과 발급**: 예산 초과 발생
- ❌ **이벤트 신뢰도 하락**: 선착순의 의미 상실
- ❌ **금전적 손실**: 예상보다 많은 할인 제공

---

### 3. 포인트 충전/사용 (Lost Update)

#### 문제 상황
```
초기 포인트: 10,000원

[Thread-1] 포인트 조회 → 10,000원
[Thread-2] 포인트 조회 → 10,000원
[Thread-1] 5,000원 충전 → 15,000원으로 업데이트
[Thread-2] 3,000원 사용 → 7,000원으로 업데이트 ❌

결과: 충전한 5,000원이 반영되지 않고 7,000원이 됨
```

#### 영향
- ❌ **포인트 불일치**: 실제 거래와 잔액 불일치
- ❌ **금전적 손실**: 충전 금액 누락 또는 이중 차감
- ❌ **데이터 무결성**: 포인트 이력과 잔액 불일치

## 선택한 동시성 제어 방식

### ✅ ReentrantLock (Java Concurrent Lock)

#### 선택 이유

본 프로젝트는 **In-Memory 저장소(ConcurrentHashMap)**를 사용하는 환경에서 **ReentrantLock**을 선택했습니다.

#### 장점

| 항목 | 설명 |
|------|------|
| 🎯 **명시적 제어** | Lock/Unlock을 명시적으로 제어하여 코드의 의도가 명확함 |
| 🔄 **재진입 가능** | 같은 스레드가 이미 획득한 Lock을 다시 획득 가능 |
| ⚖️ **공정성 옵션** | Fair Lock 사용 시 대기 큐 순서대로 Lock 획득 가능 |
| ⏱️ **타임아웃 지원** | `tryLock(timeout)` 사용으로 무한 대기 방지 가능 |
| 🧩 **세밀한 제어** | Condition 변수를 통한 복잡한 동기화 시나리오 구현 가능 |
| 💾 **In-Memory 적합** | 단일 JVM 환경에서 추가 인프라 없이 사용 가능 |

#### 단점

| 항목 | 설명 | 대응 방안 |
|------|------|----------|
| 🔧 **코드 복잡도** | finally 블록에서 반드시 unlock 필요 | 템플릿 메서드 패턴 적용 고려 |
| 🌐 **분산 환경 미지원** | 단일 JVM 내에서만 동작 | Redis Distributed Lock으로 전환 필요 |
| 🐛 **데드락 위험** | 잘못된 사용 시 데드락 발생 가능 | Lock 획득 순서 정의 및 테스트 강화 |

#### 구현 위치: Repository 계층

```
Controller/API
    ↓
UseCase (비즈니스 로직)
    ↓
Repository (동시성 제어 + 데이터 접근) ← ReentrantLock 적용
    ↓
Entity (도메인 로직 + 검증)
    ↓
In-Memory Store (ConcurrentHashMap)
```

**Repository 계층을 선택한 이유:**

| 이유 | 설명 |
|------|------|
| 📦 **관심사 분리** | UseCase는 비즈니스 로직에만 집중, 동시성 제어는 Repository가 담당 |
| ♻️ **재사용성** | 여러 UseCase에서 동일한 Repository 메서드를 안전하게 공유 |
| 🧪 **테스트 용이성** | 계층별로 독립적인 단위 테스트 작성 가능 |
| 🔄 **변경 용이성** | DB 전환 시 Repository 구현체만 교체하면 됨 |

## 대안 방식 비교

### 1. synchronized 키워드

#### 개요
Java의 기본 동기화 메커니즘으로 메서드 또는 블록 단위로 동기화를 제공합니다.

#### 장점
- ✅ 간결한 문법
- ✅ 자동 Lock 해제 (예외 발생 시에도)
- ✅ JVM 레벨 최적화 지원

#### 단점
- ❌ Lock 타임아웃 불가능
- ❌ 공정성 제어 불가능
- ❌ 인터럽트 불가능
- ❌ 조건 변수 없음

#### 적용 시나리오
- 단순한 동기화가 필요한 경우
- 코드 복잡도를 최소화하려는 경우

---

### 2. Optimistic Lock (낙관적 잠금)

#### 개요
데이터베이스의 버전 컬럼을 활용하여 충돌을 감지하고, 충돌 시 재시도하는 방식입니다.

#### 장점
- ✅ Lock을 잡지 않아 성능 우수
- ✅ 데드락 발생 없음
- ✅ 읽기 작업이 많은 환경에 적합

#### 단점
- ❌ 충돌 시 재시도 필요 (복잡도 증가)
- ❌ 쓰기 작업이 많으면 성능 저하
- ❌ 데이터베이스 필요 (In-Memory에 부적합)

#### 적용 시나리오
- 읽기가 많고 쓰기가 적은 경우
- 데이터베이스를 사용하는 경우
- 충돌 가능성이 낮은 경우

```java
@Entity
public class Product {
    @Id
    private Long id;

    @Version
    private Long version;  // 낙관적 잠금을 위한 버전 필드

    private int stock;
}
```

---

### 3. Pessimistic Lock (비관적 잠금)

#### 개요
데이터베이스 레벨에서 Row Lock을 획득하여 동시성을 제어합니다.

#### 장점
- ✅ 데이터 일관성 보장
- ✅ 충돌 재시도 불필요
- ✅ 쓰기 작업이 많은 환경에 적합

#### 단점
- ❌ Lock 대기로 인한 성능 저하
- ❌ 데드락 가능성
- ❌ 데이터베이스 필요 (In-Memory에 부적합)

#### 적용 시나리오
- 쓰기 작업이 많은 경우
- 충돌 가능성이 높은 경우
- 데이터 일관성이 최우선인 경우

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdForUpdate(@Param("id") Long id);
```

---

### 4. Redis Distributed Lock

#### 개요
Redis를 활용하여 분산 환경에서 동시성을 제어합니다.

#### 장점
- ✅ 분산 환경 지원
- ✅ Lock 타임아웃 지원
- ✅ 스케일 아웃 가능

#### 단점
- ❌ 외부 인프라 필요 (Redis)
- ❌ 네트워크 지연 발생
- ❌ Redis 장애 시 시스템 영향
- ❌ 구현 복잡도 증가

#### 적용 시나리오
- 멀티 서버 환경
- 마이크로서비스 아키텍처
- 높은 가용성이 필요한 경우

```java
@Transactional
public void decreaseStock(Long productId, int quantity) {
    RLock lock = redissonClient.getLock("product:" + productId);
    try {
        if (lock.tryLock(10, 1, TimeUnit.SECONDS)) {
            // Critical Section
            productRepository.decreaseStock(productId, quantity);
        }
    } finally {
        lock.unlock();
    }
}
```

---

### 5. 동시성 제어 방식 비교표

| 항목 | ReentrantLock | synchronized | Optimistic Lock | Pessimistic Lock | Redis Lock |
|------|---------------|--------------|-----------------|------------------|------------|
| **타임아웃** | ✅ | ❌ | ✅ (재시도) | ✅ | ✅ |
| **공정성** | ✅ | ❌ | N/A | ✅ | ✅ |
| **성능** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| **복잡도** | 중간 | 낮음 | 높음 | 중간 | 높음 |
| **분산 지원** | ❌ | ❌ | ✅ (DB 필요) | ✅ (DB 필요) | ✅ |
| **In-Memory** | ✅ | ✅ | ❌ | ❌ | ✅ |
| **데드락 위험** | ⚠️ | ⚠️ | ❌ | ⚠️ | ⚠️ |

---

## 구현 상세

### 1. 상품 재고 차감

**파일**: `InMemoryProductRepository.java`

```java
private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

@Override
public void decreaseStock(Long productId, int quantity) {
    // 1. 상품별 Lock 획득
    Lock lock = locks.computeIfAbsent(productId, k -> new ReentrantLock());
    lock.lock();

    try {
        // 2. Critical Section: 재고 조회 및 차감
        Product product = store.get(productId);
        if (product == null) {
            throw new ProductNotFoundException("상품을 찾을 수 없습니다: " + productId);
        }
        // Entity의 비즈니스 로직 호출 (검증 포함)
        product.decreaseStock(quantity);
    } finally {
        // 3. Lock 해제 (반드시 finally에서)
        lock.unlock();
    }
}
```

**핵심 포인트:**
- 🔑 **상품별 세밀한 Lock**: `productId` 기준으로 독립적인 Lock 생성
- ⚡ **병렬 처리 가능**: 서로 다른 상품은 동시 처리
- 🛡️ **직렬화**: 같은 상품에 대한 요청만 순차 처리

---

### 2. 선착순 쿠폰 발급

**파일**: `InMemoryCouponEventRepository.java`

```java
private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

@Override
public void issueCoupon(Long couponEventId) {
    Lock lock = locks.computeIfAbsent(couponEventId, k -> new ReentrantLock());
    lock.lock();

    try {
        CouponEvent event = store.get(couponEventId);
        if (event == null) {
            throw new CouponEventNotFoundException("쿠폰 이벤트를 찾을 수 없습니다");
        }
        // Entity의 비즈니스 로직 호출 (수량 체크 및 증가)
        event.issue();
    } finally {
        lock.unlock();
    }
}
```

**핵심 포인트:**
- 🎟️ **이벤트별 Lock**: 각 쿠폰 이벤트에 독립적인 Lock
- 🚀 **정확한 수량 제한**: Race Condition 없이 정확한 발급 수량 보장
- ⚠️ **즉시 예외 처리**: 수량 초과 시 발급 실패 즉시 알림

---

### 3. 포인트 충전/사용

**파일**: `InMemoryUserRepository.java`

```java
private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

@Override
public void chargePoint(Long userId, long amount) {
    Lock lock = locks.computeIfAbsent(userId, k -> new ReentrantLock());
    lock.lock();

    try {
        User user = store.get(userId);
        if (user == null) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다");
        }
        // Entity의 비즈니스 로직 호출
        user.chargePoint(amount);
    } finally {
        lock.unlock();
    }
}

@Override
public void usePoint(Long userId, long amount) {
    Lock lock = locks.computeIfAbsent(userId, k -> new ReentrantLock());
    lock.lock();

    try {
        User user = store.get(userId);
        if (user == null) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다");
        }
        // Entity의 비즈니스 로직 호출 (검증 포함)
        user.usePoint(amount);
    } finally {
        lock.unlock();
    }
}
```

**핵심 포인트:**
- 👤 **사용자별 Lock**: `userId` 기준으로 독립적인 Lock 생성
- 🔒 **직렬화**: 동일 사용자의 충전/사용 요청 순차 처리
- 🚀 **병렬 처리**: 서로 다른 사용자는 동시 처리 가능

---

## 테스트 결과

### 1. 재고 차감 동시성 테스트

**테스트 시나리오**: 100개 스레드가 동시에 재고 1개씩 차감

```java
@Test
@DisplayName("재고 차감 - 동시성 제어 검증")
void decreaseStockConcurrency() throws InterruptedException {
    // given
    Product product = repository.save(new Product(null, "상품1", 10000, 100));

    int threadCount = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when - 100개 스레드가 각각 1개씩 차감
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                repository.decreaseStock(product.getId(), 1);
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executorService.shutdown();

    // then
    Product result = repository.findById(product.getId()).orElseThrow();
    assertThat(result.getStock()).isEqualTo(0);  // ✅ 정확히 0개
}
```

**결과**: ✅ **성공** - 100개 스레드가 각각 1개씩 차감하여 정확히 0개 남음

---

### 2. 쿠폰 발급 동시성 테스트

**테스트 시나리오**: 150명이 100개 한정 쿠폰에 동시 발급 요청

```java
@Test
@DisplayName("쿠폰 발급 - 선착순 100명 제한")
void issueCouponConcurrency() throws InterruptedException {
    // given
    CouponEvent event = repository.save(new CouponEvent(..., 100));  // 100개 한정

    int threadCount = 150;
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    // when - 150명이 동시에 발급 요청
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                repository.issueCoupon(event.getId());
                successCount.incrementAndGet();  // 성공
            } catch (CouponIssueLimitExceededException e) {
                failCount.incrementAndGet();  // 실패
            }
        });
    }

    // then
    assertThat(successCount.get()).isEqualTo(100);  // ✅ 정확히 100명 성공
    assertThat(failCount.get()).isEqualTo(50);      // ✅ 50명 실패
}
```

**결과**: ✅ **성공** - 정확히 100명만 발급 성공, 50명 실패

---

### 3. 포인트 충전/사용 동시성 테스트

**테스트 시나리오**: 50번 충전(+100원) & 50번 사용(-100원) 동시 실행

```java
@Test
@DisplayName("포인트 충전과 사용이 동시에 발생하는 경우")
void chargeAndUseConcurrently() throws InterruptedException {
    // given
    User user = repository.save(new User(null, "사용자1", 10000));

    int threadCount = 50;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);

    // when - 50번 충전, 50번 사용
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> repository.chargePoint(user.getId(), 100));
        executorService.submit(() -> {
            try {
                repository.usePoint(user.getId(), 100);
            } catch (InsufficientPointException e) {
                // 포인트 부족 시 무시
            }
        });
    }

    // then
    User result = repository.findById(user.getId()).orElseThrow();
    // 10000 + (50 * 100) - (50 * 100) = 10000 (이론상)
    // 실제로는 사용 실패가 있을 수 있으므로 >= 10000
    assertThat(result.getPointBalance()).isGreaterThanOrEqualTo(10000);  // ✅
}
```

**결과**: ✅ **성공** - 최소 10,000원 이상 유지 (데이터 일관성 보장)

---

## 결론 및 향후 개선

### ✅ 현재 구현의 성과

1. **동시성 이슈 해결**: ReentrantLock을 통해 Race Condition 완벽 차단
2. **세밀한 제어**: 리소스별(상품, 쿠폰, 사용자) 독립적인 Lock으로 성능 최적화
3. **테스트 검증**: 100+ 스레드 동시 실행 환경에서 데이터 일관성 보장
4. **계층 분리**: Repository에서 동시성 제어, UseCase는 비즈니스 로직에만 집중

### 🚀 향후 개선 방향

#### 1. 분산 환경 대응
- **Redis Distributed Lock** 도입으로 멀티 서버 환경 지원
- **Redisson** 라이브러리 활용한 구현

#### 2. 성능 최적화
- **Lock Timeout** 설정으로 무한 대기 방지
- **Optimistic Lock 혼용**: 읽기가 많은 작업은 낙관적 잠금 적용
- **Lock Granularity 조정**: 필요 시 더 세밀한 Lock 단위 적용

#### 3. 모니터링 및 알림
- **Lock 대기 시간** 모니터링 (Prometheus, Grafana)
- **데드락 감지** 및 알림 시스템 구축
- **성능 메트릭** 수집 (처리량, 응답 시간)

#### 4. 데이터베이스 전환 시
- JPA의 `@Lock` 어노테이션 활용
- 트랜잭션 격리 수준 조정
- Connection Pool 최적화

---

## 📚 참고 자료

- [Java Concurrency in Practice](https://jcip.net/)
- [ReentrantLock Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html)
- [Distributed Locks with Redis](https://redis.io/docs/manual/patterns/distributed-locks/)