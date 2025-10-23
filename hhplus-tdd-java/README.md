# hhplus-tdd-java 

## 동시성 정리
1. synchrorized 키워드
2. synchronized 블록
3. ReentrantLock 사용
4. DB 비관적 락 (Pessimistic Lock)
5. DB 낙관적 락 (Optimistic Lock)
6. 분산 락 (Redis, Reddsion)

### 0. 동시 처리 Lost Update 문제
동시성 처리를 하지 않으면 여러 스레드가 동일한 초기값을 읽고 덮어쓰는 Lost Update 문제가 발생한다.
동시성 제어를 하기 위해서는 위와 같은 방법이 있고 하단에 상세 설명을 정리한다.

### 1. synchronized 키워드
synchonzied 키워드는 정말 구현이 간단하지만 성능 저하 및 세밀한 객체별 제어 불가
- 장점
    - 구현은 정말 간단!
    - JVM 레벨에서 보장
- 단점
    - 메서드 전체에 락이 걸려 성능 저하
    - 같은 인스턴스 내에서만 동기화 (여러 서버 환경에서 무용)
    - 모든 userId에 대해 하나의 락 사용(userId 별 세밀한 제어 불가)
```java
    public synchronized UserPoint charge(long userId, long chargeAmount) {
~
        return savedUserPoint;
    }
```

## 2. synchronized 블록 (객체별 락)
synchorinzed 블록은 객체별 락을 잡을 수 있지만 메모리 누수 및 단일 서버에서만 작동
- 장점
    - userId 별로 세밀한 락 제어
    - 다른 사용자는 동시 처리 가능
- 단점
    - 메모리 누수 가능성 (락 객체 제거 필요)
    - 단일 서버에서만 작동
```java
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    public UserPoint charge(long userId, long chargeAmount) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        synchronized(lock) {
~
			  return savedUserPoint;
        }
    }
```

## 3. ReentrantLock
synchonized보다 lock, unlock을 할 수 있어서 유연하지만 명시적으로 unlock이 필요하다.
- 장점
    - tryLock()으로 타임아웃 설정 가능
    - synchonzied 보다 유연
- 단점
    - 명시적 unlock 필요 (실수 가능성 존재)
    - 단일 서버에서만 작동
```java
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public UserPoint charge(long userId, long chargeAmount) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();
        try {
~
    return savedUserPoint;
        } finally {
            lock.unlock();
        }
    }
```

### 4. DB 비관적 락
하단과 같이 JPA 사용 시 애노테이션으로 설정 가능
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query(”SELECT ~)
- 장점
    - DB 레벨 락 보장
    - 다중 서버 환경 작동
    - 데이터 정합성 강력 보장
- 단점
    - DB 성능 영향
    - 데드락 가능성

### 5. DB 낙관적 락
version 필드를 사용해서 업데이트 시 버전 체크
- 장점
    - 충돌 적은 환경 효율적
    - DB 락 시간 최소화
- 단점
    - 충돌 시 재시도 로직 필요

### 6. 분산 락 (Redis, Reddison)
ReentrantLock과 방식은 유사해보이지만 redis는 분산 환경 적합
- 장점
    - 다중 서버 환경 작동
    - 확장성 좋음
    - TTL 설정 가능
- 단점
    - 외부 의존성 추가 (Redis)
    - 네트워크 레이턴시
    - 복잡도 증가
```java
RLock lock = redissonClient.getLock("point:lock:" + userId);
lock.lock();
try {
// 동시성 제어
} finally {
   lock.unlock();
}
```