# 이커머스 서비스 API 설계 문서

## 프로젝트 개요

이 프로젝트는 이커머스 서비스의 핵심 기능을 설계하고 문서화한 프로젝트입니다. 상품 카탈로그, 재고 관리, 주문/결제 프로세스, 선착순 쿠폰 시스템 등을 포함하며, 동시성 제어와 트랜잭션 관리를 고려한 설계를 제공합니다.

---

## 📚 문서 구조

본 프로젝트는 다음과 같은 체계적인 문서로 구성되어 있습니다:

### 1. **요구사항 명세서** ([requirements.md](./requirements.md))
- **기능적 요구사항**: 상품 관리, 주문/결제, 쿠폰 시스템, 데이터 연동 등
- **비기능적 요구사항**: 성능, 데이터 정합성, 가용성, 확장성, 보안 등
- **핵심 비즈니스 규칙**: 재고 관리, 쿠폰 시스템, 결제 처리 규칙
- **우선순위**: 높음/중간/낮음 단계별 구현 가이드

### 2. **사용자 스토리** ([user-stories.md](./user-stories.md))
- **18개 사용자 스토리**: US-PROD-001 ~ US-DATA-002
- **Acceptance Criteria**: 각 스토리별 상세 검증 기준
- **Phase별 분류**: 핵심 기능 → 부가 기능 → 확장 기능
- **스토리 포인트 추정**: 복잡도 기반 개발 공수 예측

### 3. **API 명세서** ([api-specification.md](./api-specification.md))
- **RESTful API 설계**: 7개 도메인, 15개 엔드포인트
- **상세 Request/Response**: JSON 스키마 및 예시
- **에러 코드 정의**: 8개 카테고리별 에러 코드 및 HTTP 상태
- **curl 예시**: 모든 API에 대한 실행 가능한 명령어

### 4. **데이터 모델** ([data-model.md](./data-model.md))
- **ERD**: Mermaid 기반 엔티티 관계 다이어그램
- **7개 테이블 설계**: users, products, cart_items, orders, order_items, coupon_events, user_coupons
- **인덱스 전략**: 성능 최적화를 위한 7개 핵심 인덱스
- **DDL**: 즉시 실행 가능한 테이블 생성 스크립트
- **샘플 데이터**: 테스트용 초기 데이터

### 5. **시퀀스 다이어그램** ([sequence-diagrams.md](./sequence-diagrams.md))
- **4개 핵심 플로우**: 상품 조회, 장바구니, 주문/결제, 쿠폰 발급
- **동시성 제어 패턴**: FOR UPDATE를 활용한 비관적 락
- **트랜잭션 범위**: 원자성 보장을 위한 트랜잭션 경계
- **에러 처리 시나리오**: 재고 부족, 잔액 부족, 쿠폰 만료 등

### 6. **플로우차트** ([flowcharts.md](./flowcharts.md))
- **전체 시스템 플로우**: End-to-End 사용자 여정
- **도메인별 플로우**: 상품, 장바구니, 주문/결제, 쿠폰 시스템
- **동시성 제어 플로우**: 비관적 락 처리 과정 시각화
- **에러 처리 플로우**: 공통 에러 처리 패턴

---

## 🎯 핵심 기능

### 1. 상품 관리
- **상품 목록 조회** (페이지네이션 지원)
- **상품 상세 조회** (실시간 재고 정보)
- **인기 상품 조회** (최근 3일간 판매량 Top 5)

### 2. 장바구니
- **장바구니 추가/조회/수정/삭제**
- **재고 확인 및 총액 계산**
- **중복 상품 자동 수량 증가**

### 3. 주문 및 결제
- **주문 생성** (장바구니 기반)
- **잔액 기반 결제**
- **쿠폰 할인 적용**
- **트랜잭션 기반 원자성 보장**
  - 주문 생성 → 재고 차감 → 잔액 차감 → 쿠폰 사용 처리

### 4. 쿠폰 시스템
- **선착순 쿠폰 발급** (동시성 제어)
- **쿠폰 목록 조회** (사용 가능/만료/사용됨 구분)
- **쿠폰 검증** (유효기간, 사용 여부)
- **중복 발급 방지**

### 5. 잔액 관리
- **잔액 조회**
- **잔액 충전** (동시성 제어)

---

## 🔐 동시성 제어 전략

### synchronized 또는 ReentrantLock 적용 대상

```java
// 1. 재고 차감 시
private final ReentrantLock stockLock = new ReentrantLock();

public void decreaseStock(Long productId, int quantity) {
    stockLock.lock();
    try {
        Product product = productRepository.findById(productId);
        product.decreaseStock(quantity);
        productRepository.save(product);
    } finally {
        stockLock.unlock();
    }
}

// 2. 쿠폰 발급 시
private final ReentrantLock couponLock = new ReentrantLock();

public void issueCoupon(Long couponEventId, Long userId) {
    couponLock.lock();
    try {
        CouponEvent event = couponEventRepository.findById(couponEventId);
        event.increaseIssuedQuantity();
        // 쿠폰 발급 처리
    } finally {
        couponLock.unlock();
    }
}

// 3. 잔액 차감/충전 시
public synchronized void updateBalance(Long userId, long amount) {
    User user = userRepository.findById(userId);
    user.updateBalance(amount);
    userRepository.save(user);
}
```

### 적용 이유
- **재고 부족 방지**: 동시 주문 시 재고 음수 방지
- **쿠폰 초과 발급 방지**: 발급 한도 초과 방지
- **잔액 정합성**: 동시 충전/차감 시 정확한 잔액 유지

---

## 📊 주요 API 엔드포인트

### 상품 API
```
GET    /api/v1/products              # 상품 목록 조회
GET    /api/v1/products/{id}         # 상품 상세 조회
GET    /api/v1/products/popular      # 인기 상품 조회
```

### 장바구니 API
```
GET    /api/v1/cart                  # 장바구니 조회
POST   /api/v1/cart/items            # 장바구니에 상품 추가
PATCH  /api/v1/cart/items/{id}       # 장바구니 상품 수량 변경
DELETE /api/v1/cart/items/{id}       # 장바구니 상품 삭제
```

### 주문 API
```
POST   /api/v1/orders                # 주문 생성 (결제 포함)
GET    /api/v1/orders                # 주문 내역 조회
GET    /api/v1/orders/{id}           # 주문 상세 조회
```

### 결제 API
```
GET    /api/v1/balance               # 잔액 조회
POST   /api/v1/balance/charge        # 잔액 충전
```

### 쿠폰 API
```
POST   /api/v1/coupons/{eventId}/issue  # 쿠폰 발급
GET    /api/v1/coupons                   # 보유 쿠폰 조회
GET    /api/v1/coupon-events             # 쿠폰 이벤트 목록 조회
```

---

## 🗄️ 데이터베이스 설계

### ERD 개요

```
USERS (사용자, 잔액)
  ├─→ CART_ITEMS (장바구니)
  ├─→ ORDERS (주문)
  └─→ USER_COUPONS (보유 쿠폰)

PRODUCTS (상품, 재고)
  ├─→ CART_ITEMS
  └─→ ORDER_ITEMS (주문 상품 상세)

COUPON_EVENTS (쿠폰 이벤트)
  └─→ USER_COUPONS
```

### 테이블 요약

| 테이블 | 주요 컬럼 | 동시성 제어 | 용도 |
|--------|----------|-----------|------|
| users | id, name, balance | synchronized/ReentrantLock | 사용자 및 잔액 관리 |
| products | id, name, price, stock | synchronized/ReentrantLock | 상품 및 재고 관리 |
| cart_items | user_id, product_id, quantity | - | 장바구니 |
| orders | user_id, total_amount, final_amount, status | - | 주문 이력 |
| order_items | order_id, product_id, quantity, price | - | 주문 상품 상세 |
| coupon_events | name, total_quantity, issued_quantity | synchronized/ReentrantLock | 쿠폰 이벤트 |
| user_coupons | user_id, coupon_event_id, is_used | - | 발급된 쿠폰 |

### 핵심 인덱스

```sql
-- 장바구니: 중복 방지 + 빠른 조회
CREATE UNIQUE INDEX idx_cart_user_product ON cart_items(user_id, product_id);

-- 주문: 사용자별 최근 주문 조회
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

-- 주문: 인기 상품 집계
CREATE INDEX idx_orders_created ON orders(created_at);

-- 쿠폰: 중복 발급 방지
CREATE UNIQUE INDEX idx_user_coupon_unique ON user_coupons(user_id, coupon_event_id);
```

---

## 🔄 트랜잭션 처리

### 주문 생성 트랜잭션 (10단계)

```
트랜잭션 시작
  ├─ 1. 장바구니 조회
  ├─ 2. 재고 확인 (FOR UPDATE)
  ├─ 3. 쿠폰 검증 (선택적)
  ├─ 4. 잔액 확인 (FOR UPDATE)
  ├─ 5. 주문 생성 (orders)
  ├─ 6. 주문 항목 저장 (order_items)
  ├─ 7. 재고 차감 (products.stock)
  ├─ 8. 잔액 차감 (users.balance)
  ├─ 9. 쿠폰 사용 처리 (user_coupons.is_used)
  └─ 10. 장바구니 삭제
트랜잭션 커밋 (모든 단계 성공 시)
```

**실패 시**: 전체 롤백 (원자성 보장)

---

## ⚠️ 에러 처리

### HTTP 상태 코드

| 상태 코드 | 사용 시점 | 예시 |
|----------|---------|------|
| 200 OK | 조회/수정 성공 | 상품 목록 조회, 잔액 충전 |
| 201 Created | 생성 성공 | 주문 생성, 쿠폰 발급 |
| 204 No Content | 삭제 성공 | 장바구니 상품 삭제 |
| 400 Bad Request | 잘못된 요청 | 잔액 부족, 쿠폰 만료 |
| 404 Not Found | 리소스 없음 | 상품/주문/쿠폰 없음 |
| 409 Conflict | 리소스 충돌 | 재고 부족, 쿠폰 소진 |
| 500 Internal Error | 서버 오류 | DB 에러, 예상치 못한 오류 |

### 주요 에러 코드

```json
{
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "재고가 부족합니다.",
    "details": "요청 수량: 10, 현재 재고: 5"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**주요 에러 코드**:
- `PRODUCT_NOT_FOUND`: 상품 없음
- `INSUFFICIENT_STOCK`: 재고 부족
- `INSUFFICIENT_BALANCE`: 잔액 부족
- `COUPON_SOLD_OUT`: 쿠폰 소진
- `COUPON_EXPIRED`: 쿠폰 만료
- `COUPON_ALREADY_USED`: 쿠폰 이미 사용

---

## 🎨 다이어그램 미리보기

### 시퀀스 다이어그램 예시 (주문 생성)

```
고객 → API → OrderService → DB

1. POST /orders
2. 트랜잭션 시작
3. 장바구니 조회
4. 재고 확인 (FOR UPDATE)
5. 쿠폰 검증
6. 잔액 확인 (FOR UPDATE)
7. 주문 생성
8. 재고/잔액 차감
9. 트랜잭션 커밋
10. 201 Created 응답
```

### 플로우차트 예시 (쿠폰 발급)

```
쿠폰 발급 요청
  ↓
트랜잭션 시작
  ↓
쿠폰 이벤트 조회 (FOR UPDATE)
  ↓
쿠폰 남아있음? ─No→ 409 Conflict (소진)
  ↓ Yes
중복 발급? ─Yes→ 409 Conflict (중복)
  ↓ No
쿠폰 발급 + 수량 증가
  ↓
트랜잭션 커밋
  ↓
201 Created
```

---

## 📋 비기능적 요구사항

### 성능
- **상품 조회**: 평균 200ms 이내
- **주문 처리**: 3초 이내
- **인기 상품 조회**: 500ms 이내
- **동시 처리**: 최소 100개 주문 동시 처리

### 데이터 정합성
- **동시성 제어**: synchronized 또는 ReentrantLock (애플리케이션 레벨)
- **트랜잭션**: ACID 속성 보장
- **원자성**: 주문-결제-재고차감 일괄 처리

### 가용성
- **외부 연동 장애 시**: 주문/결제 정상 동작
- **목표 가용성**: 99% 이상

---

## 🚀 구현 우선순위

### Phase 1 (높음) - 핵심 기능
1. 상품 목록/상세 조회
2. 주문 생성
3. 동시 주문 시 재고 정합성 보장
4. 잔액 조회/충전
5. 주문 결제 (쿠폰 미적용)

### Phase 2 (중간) - 부가 기능
1. 장바구니 CRUD
2. 선착순 쿠폰 발급/조회
3. 동시 쿠폰 발급 시 정합성 보장
4. 주문 결제 (쿠폰 적용)
5. 인기 상품 조회

### Phase 3 (낮음) - 확장 기능
1. 주문 내역 조회
2. 외부 데이터 플랫폼 연동
3. 데이터 전송 실패 처리

---

## 📖 문서 활용 가이드

### 개발자용
1. **[requirements.md](./requirements.md)** → 전체 요구사항 이해
2. **[data-model.md](./data-model.md)** → 데이터베이스 스키마 생성
3. **[api-specification.md](./api-specification.md)** → API 구현
4. **[sequence-diagrams.md](./sequence-diagrams.md)** → 비즈니스 로직 구현
5. **[flowcharts.md](./flowcharts.md)** → 프로세스 흐름 검증

### 기획자/PM용
1. **[user-stories.md](./user-stories.md)** → 사용자 스토리 및 AC 확인
2. **[flowcharts.md](./flowcharts.md)** → 전체 사용자 여정 이해
3. **[api-specification.md](./api-specification.md)** → API 기능 확인

### QA/테스터용
1. **[user-stories.md](./user-stories.md)** → 테스트 케이스 작성
2. **[api-specification.md](./api-specification.md)** → API 테스트 시나리오
3. **[sequence-diagrams.md](./sequence-diagrams.md)** → 에러 시나리오 테스트

---

## 🛠️ 기술 스택 권장사항

### Backend
- **언어**: Java 17+
- **프레임워크**: Spring Boot 3.x
- **ORM**: JPA/Hibernate
- **DB**: MySQL 8.0+

### 동시성 제어
- **애플리케이션 레벨 락**: `synchronized` 또는 `ReentrantLock`
- **트랜잭션**: `@Transactional`

### API 문서화
- **Swagger/OpenAPI**: springdoc-openapi

---

## 📝 설계 원칙

### 1. 단순성 (Simplicity)
- 핵심 기능에만 집중
- 복잡한 설계 배제
- 명확한 책임 분리

### 2. 확장성 (Scalability)
- 레이어드 아키텍처
- 도메인별 독립성
- 인덱스 기반 성능 최적화

### 3. 안정성 (Reliability)
- 트랜잭션 기반 일관성 보장
- 동시성 제어를 통한 데이터 정합성
- 명확한 에러 처리

### 4. 유지보수성 (Maintainability)
- 체계적인 문서화
- 명확한 네이밍 컨벤션
- 테스트 가능한 구조

---

## 📚 참고 문서

- [요구사항 명세서](./requirements.md)
- [사용자 스토리](./user-stories.md)
- [API 명세서](./api-specification.md)
- [데이터 모델](./data-model.md)
- [시퀀스 다이어그램](./sequence-diagrams.md)
- [플로우차트](./flowcharts.md)

---

## 📅 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2025-10-29 | 초기 문서 작성 완료 |

---

## 💡 FAQ

### Q1: FK 제약 조건이 없는 이유는?
**A**: 유연한 스키마 변경, 성능 향상, 순환 참조 해결을 위해 FK를 제거하고 애플리케이션 레벨에서 참조 무결성을 관리합니다.

### Q2: synchronized/ReentrantLock을 사용하는 이유는?
**A**: 재고, 쿠폰 수량, 잔액 등 정합성이 중요한 데이터에 대해 애플리케이션 레벨에서 동시성 제어를 통해 데이터 무결성을 보장하기 위함입니다.

### Q3: 주문 생성 시 트랜잭션 범위는?
**A**: 장바구니 조회부터 재고 차감, 잔액 차감, 쿠폰 사용까지 전체 프로세스를 하나의 트랜잭션으로 처리하여 원자성을 보장합니다.

### Q4: 인기 상품은 어떻게 집계하나요?
**A**: 최근 3일간의 `orders` 및 `order_items` 테이블을 조인하여 판매량을 집계하고 상위 5개를 반환합니다.

### Q5: 쿠폰 중복 발급은 어떻게 방지하나요?
**A**: `user_coupons` 테이블의 `(user_id, coupon_event_id)` UNIQUE 인덱스를 통해 DB 레벨에서 중복 발급을 방지합니다.

---

## 👥 기여자

이 문서는 이커머스 서비스의 체계적인 설계를 위해 작성되었습니다.

**문의사항이나 개선 제안은 이슈를 통해 공유해주세요.**
