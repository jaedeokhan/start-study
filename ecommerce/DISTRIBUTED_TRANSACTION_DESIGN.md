# STEP 16 - 분산 트랜잭션 진단 및 설계

## 목차
1. 현황 분석 (AS-IS)
2. 도메인 분리 시나리오 (TO-BE)
3. 발생 가능한 문제점
4. 분산 트랜잭션 대응 방안
5. 선택한 설계: Saga Orchestration 패턴
6. 보상 트랜잭션 설계
7. 트레이드오프 분석
8. 결론

## 1. 현황 분석 (AS-IS)

### 1.1 모놀리식 아키텍처

```
┌─────────────────────────────────────────┐
│         Spring Boot Application         │
│                                          │
│  ┌──────────────────────────────────┐  │
│  │   CreateOrderUseCase             │  │
│  │   @Transactional (단일 트랜잭션)  │  │
│  │                                   │  │
│  │  1. Cart 조회/삭제                │  │
│  │  2. Product 재고 차감             │  │
│  │  3. User 포인트 차감              │  │
│  │  4. Coupon 사용 처리              │  │
│  │  5. Order 생성                    │  │
│  │  6. OrderItem 생성                │  │
│  │  7. PointHistory 저장             │  │
│  └──────────────────────────────────┘  │
│                 ↓                        │
│       Single Database (MySQL)            │
└─────────────────────────────────────────┘
```

### 1.2 현재 트랜잭션 특징
- 원자성 보장: 모든 작업이 성공하거나 모두 롤백
- 일관성: 단일 DB 트랜잭션으로 강한 일관성 보장
- 격리성: @Transactional 레벨로 제어
- 분산 락: Redis 기반 @MultiDistributedLock 사용

### 1.3 장점

- ✅ 구현 단순
- ✅ 강한 일관성 보장
- ✅ 트랜잭션 경계 명확
- ✅ 롤백 처리 자동

### 1.4 단점

- ❌ 모든 도메인이 강하게 결합
- ❌ 확장성 제한 (수평 확장 어려움)
- ❌ 특정 도메인 장애가 전체 시스템에 영향
- ❌ 트래픽 집중 시 병목 현상

## 2. 도메인 분리 시나리오 (TO-BE)

### 2.1 서비스 분리 전략

```
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│  User Service  │  │ Product Service│  │ Coupon Service │
│                │  │                │  │                │
│ - User         │  │ - Product      │  │ - CouponEvent  │
│ - Point        │  │ - Stock        │  │ - UserCoupon   │
│ - PointHistory │  │                │  │                │
│                │  │                │  │                │
│   DB_USER      │  │  DB_PRODUCT    │  │  DB_COUPON     │
└────────────────┘  └────────────────┘  └────────────────┘
↑                   ↑                   ↑
└───────────────────┴───────────────────┘
│
┌───────────────────────┐
│   Order Service       │
│                       │
│ - Order               │
│ - OrderItem           │
│ - Cart                │
│                       │
│ - Order Orchestrator  │
│   (주문 조율자)        │
│                       │
│      DB_ORDER         │
└───────────────────────┘
```

### 2.2 배포 단위 분리

| 서비스             | 책임             | 독립 배포 | 독립 DB |
  |-----------------|----------------|-------|-------|
| User Service    | 사용자 관리, 포인트 관리 | ✅     | ✅     |
| Product Service | 상품 관리, 재고 관리   | ✅     | ✅     |
| Coupon Service  | 쿠폰 발급/사용 관리    | ✅     | ✅     |
| Order Service   | 주문 생성/조회 관리    | ✅     | ✅     |

## 3. 발생 가능한 문제점

### 3.1 부분 실패 (Partial Failure)

```
시나리오 1: 재고 차감 후 포인트 차감 실패
1. Product Service: 재고 10 → 9 (성공) ✅
2. User Service: 포인트 차감 (실패) ❌
3. Coupon Service: 쿠폰 사용 (미실행)
4. Order Service: 주문 생성 (미실행)
```
* 결과: 재고만 차감되고 주문 미생성 → 데이터 불일치

```
시나리오 2: 주문 생성 후 쿠폰 사용 실패
1. Product Service: 재고 차감 (성공) ✅
2. User Service: 포인트 차감 (성공) ✅
3. Order Service: 주문 생성 (성공) ✅
4. Coupon Service: 쿠폰 사용 (실패) ❌
```
* 결과: 주문은 생성되었지만 쿠폰 미적용 → 금액 불일치

### 3.2 네트워크 타임아웃

```
Order Service → Product Service (재고 차감 요청)
↓
[ 5초 대기 ]
↓
타임아웃 발생!
↓
실제로는 재고 차감 성공했지만
Order Service는 실패로 판단
↓
중복 요청 발생 가능 (Idempotency 문제)

3.3 동시성 문제

[사용자 A 주문]              [사용자 B 주문]
재고 조회: 10개              재고 조회: 10개
↓                            ↓
재고 차감: 9개                재고 차감: 9개
↓                            ↓
실제 재고: 9개 (잘못됨! 8개여야 함)

분산 락 없이는 Lost Update 발생
```

### 3.4 보상 처리 복잡도

주문 실패 시 보상 작업:
1. Product Service: 재고 복구
2. User Service: 포인트 환불
3. Coupon Service: 쿠폰 복구
4. Order Service: 주문 취소 처리

각 서비스의 보상 API 개발 필요 → 복잡도 2배 증가

## 4. 분산 트랜잭션 대응 방안

### 4.1 Two-Phase Commit (2PC)

                      [Coordinator]
                           │
           ┌───────────────┼───────────────┐
           ↓               ↓               ↓
      [Product]        [User]         [Coupon]
           │               │               │
      Phase 1: Prepare (투표)
           │               │               │
        ✅ Yes          ✅ Yes          ✅ Yes
           │               │               │
           └───────────────┴───────────────┘
                           │
      Phase 2: Commit (실행)
                           │
           ┌───────────────┼───────────────┐
           ↓               ↓               ↓
        Commit          Commit          Commit

장점
- ✅ 강한 일관성 보장
- ✅ ACID 속성 유지

단점
- ❌ 블로킹 프로토콜 (성능 저하)
- ❌ 코디네이터 단일 장애점
- ❌ 복잡한 구현
- ❌ 확장성 제한

### 4.2 Saga 패턴 - Choreography

```
Order Service          Product Service         User Service
│                       │                       │
│─── OrderCreated ────→ │                       │
│   Event               │                       │
│                       │─ 재고 차감             │
│                       │                       │
│                       │── StockReduced ─────→ │
│                       │   Event               │
│                       │                       │─ 포인트 차감
│                       │                       │
│                       │                       │
│←────────────────────────── PointDeducted ─────│
│                       │   Event               │
│─ 주문 완료             │                       │
```

장점
- ✅ 느슨한 결합
- ✅ 서비스 독립성
- ✅ 확장 용이

단점
- ❌ 전체 흐름 파악 어려움
- ❌ 순환 의존 가능성
- ❌ 디버깅 어려움

### 4.3 Saga 패턴 - Orchestration (✅ 선택)

                      [Order Orchestrator]
                              │
                      주문 요청 수신
                              │
            ┌─────────────────┼─────────────────┐
            ↓                 ↓                 ↓
      재고 차감 요청      포인트 차감 요청   쿠폰 사용 요청
       (Product)           (User)            (Coupon)
            │                 │                 │
         ✅ 성공            ✅ 성공            ✅ 성공
            │                 │                 │
            └─────────────────┴─────────────────┘
                              │
                        주문 생성 완료

장점
- ✅ 중앙 집중식 흐름 제어
- ✅ 전체 로직 파악 용이
- ✅ 보상 처리 명확
- ✅ 테스트 용이

단점
- ❌ Orchestrator 복잡도
- ❌ Orchestrator 장애 시 영향

### 4.4 Outbox 패턴 (신뢰성 보장)

```
[Order Service]
│
├─ Order 저장 (Local Transaction)
│
└─ Outbox 저장 (같은 트랜잭션)
│
↓
[Outbox Polling]
│
├─→ Kafka/RabbitMQ
│
└─→ 다른 서비스들에게 이벤트 전달
```

## 5. 선택한 설계: Saga Orchestration + Outbox

### 5.1 전체 아키텍처

```
┌─────────────────────────────────────────────────────┐
│              Order Service (Orchestrator)            │
│                                                      │
│  ┌────────────────────────────────────────────┐   │
│  │      CreateOrderOrchestrator               │   │
│  │                                             │   │
│  │  1. 재고 검증 → Product Service             │   │
│  │  2. 포인트 검증 → User Service              │   │
│  │  3. 쿠폰 검증 → Coupon Service              │   │
│  │  4. 재고 차감 → Product Service             │   │
│  │  5. 포인트 차감 → User Service              │   │
│  │  6. 쿠폰 사용 → Coupon Service              │   │
│  │  7. 주문 생성 → Order Repository            │   │
│  │                                             │   │
│  │  ⚠️ 실패 시: 보상 트랜잭션 실행             │   │
│  └────────────────────────────────────────────┘   │
│                                                      │
│  ┌────────────────────────────────────────────┐   │
│  │         Compensation Handler               │   │
│  │                                             │   │
│  │  - 재고 복구                                 │   │
│  │  - 포인트 환불                               │   │
│  │  - 쿠폰 복구                                 │   │
│  └────────────────────────────────────────────┘   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 5.2 주문 생성 흐름도

```
┌─────────┐
│  START  │
└────┬────┘
│
↓
┌─────────────────┐
│ 1. 장바구니 조회 │
└────┬────────────┘
│
↓
┌──────────────────────┐        실패 → 주문 실패 응답
│ 2. Product Service   │         ↓
│    재고 검증/차감     │───────→ FAIL
└────┬─────────────────┘         ↑
│                            │
↓                            │
┌──────────────────────┐         │
│ 3. User Service      │         │
│    포인트 검증/차감   │───────→ 보상: 재고 복구
└────┬─────────────────┘         ↑
│                            │
↓                            │
┌──────────────────────┐         │
│ 4. Coupon Service    │         │
│    쿠폰 사용          │───────→ 보상: 재고/포인트 복구
└────┬─────────────────┘         ↑
│                            │
↓                            │
┌──────────────────────┐         │
│ 5. Order 생성         │         │
│    (Local DB)         │───────→ 보상: 전체 롤백
└────┬─────────────────┘
│
↓
┌──────────────────────┐
│ 6. Outbox 저장        │
│    (이벤트 발행)      │
└────┬─────────────────┘
│
↓
┌─────────┐
│   END   │
└─────────┘
```

## 6. 보상 트랜잭션 설계

### 6.1 보상 시나리오별 처리

| 실패 단계        | 보상 작업          | 우선순위     |
  |--------------|----------------|----------|
| 1. 재고 차감 실패  | 없음 (아직 변경 없음)  | -        |
| 2. 포인트 차감 실패 | 재고 복구          | High     |
| 3. 쿠폰 사용 실패  | 재고 복구 + 포인트 환불 | High     |
| 4. 주문 생성 실패  | 전체 롤백          | Critical |

### 6.2 보상 트랜잭션 구현

```java
public class OrderCompensationHandler {

      public void compensate(OrderSagaState state) {
          switch (state.getFailedStep()) {
              case POINT_DEDUCTION:
                  // 재고만 복구
                  productService.rollbackStock(state.getReservationId());
                  break;

              case COUPON_USE:
                  // 포인트 + 재고 복구
                  userService.rollbackPoint(state.getReservationId());
                  productService.rollbackStock(state.getReservationId());
                  break;

              case ORDER_CREATION:
                  // 전체 롤백
                  couponService.rollbackCoupon(state.getReservationId());
                  userService.rollbackPoint(state.getReservationId());
                  productService.rollbackStock(state.getReservationId());
                  break;
          }
      }
}
```

### 6.3 보상 실패 시 처리

```
보상 트랜잭션 실패 → Dead Letter Queue (DLQ)
↓
재시도 큐 (3회)
↓
수동 처리 알림
↓
관리자 대시보드
```

## 7. 트레이드오프 분석

### 7.1 복잡도 비교

| 항목      | 모놀리식 | 분산 (Saga) |
  |---------|------|-----------|
| 코드 복잡도  | ⭐    | ⭐⭐⭐⭐      |
| 운영 복잡도  | ⭐    | ⭐⭐⭐⭐⭐     |
| 디버깅 난이도 | ⭐    | ⭐⭐⭐⭐      |
| 테스트 복잡도 | ⭐⭐   | ⭐⭐⭐⭐      |

### 7.2 일관성 vs 가용성 (CAP)

```
┌─────────────────────────────────────┐
│        모놀리식 (CP)                 │
│                                     │
│  일관성: ★★★★★                    │
│  가용성: ★★☆☆☆                    │
│  확장성: ★☆☆☆☆                    │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│    분산 Saga (AP - 최종 일관성)      │
│                                     │
│  일관성: ★★★☆☆ (최종 일관성)       │
│  가용성: ★★★★★                    │
│  확장성: ★★★★★                    │
└─────────────────────────────────────┘
```

### 7.3 장애 영향도

* 모놀리식
   * User Service 장애 → 전체 시스템 다운

* 분산 시스템 (Circuit Breaker 적용)
   * User Service 장애 → 포인트 결제 실패
→ 다른 결제 수단 제시 (카드 등)
→ 일부 기능 유지

### 7.4 비용 분석

| 항목       | 모놀리식   | 분산     |
  |----------|--------|--------|
| 개발 비용    | Low    | High   |
| 운영 비용    | Low    | High   |
| 인프라 비용   | Medium | High   |
| 장애 복구 비용 | High   | Medium |
| 확장 비용    | High   | Low    |

## 8. 결론
현재는 MSA로 가게 된다면 초기에는 복잡도 증가, 운영 부담, 개발 비용 등을 고려하여 모놀리식 모듈 구조를 유지하면서 이벤트 기반 아키텍처만 도입하는 것이 현실적입니다.
향후 규모가 커지거나 확장성있게 개발이 필요하다면 이커머스 시스템은 Saga Orchestration + Outbox 패턴을 적용하여 점진적으로 분산 시스템으로
전환하는 것이 좋을 것 같습니다.

- ✅ 데이터 일관성 보장 (최종 일관성)
- ✅ 확장 가능한 아키텍처
- ✅ 장애 격리
- ✅ 독립 배포 가능