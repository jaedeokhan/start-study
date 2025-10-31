# 이커머스 서비스 시퀀스 다이어그램

## 목차
1. [상품 조회](#1-상품-조회)
2. [장바구니](#2-장바구니)
3. [주문 및 결제](#3-주문-및-결제)
4. [쿠폰 발급](#4-쿠폰-발급)

---

## 1. 상품 조회

### 1.1 상품 목록 조회

```mermaid
sequenceDiagram
    actor 고객
    participant Controller as ProductController
    participant Service as ProductService
    participant Repository as ProductRepository

    고객->>Controller: GET /api/v1/products?page=0&size=20
    activate Controller

    Controller->>Service: getProducts(page, size)
    activate Service

    Service->>Repository: findAll(PageRequest)
    activate Repository
    Repository-->>Service: Page<Product>
    deactivate Repository

    Service->>Service: 응답 변환
    Service-->>Controller: ProductListResponse
    deactivate Service

    Controller-->>고객: 200 OK
    deactivate Controller
```

**Related**: US-PROD-001, FR-PROD-001~005

---

### 1.2 인기 상품 조회 (최근 3일 Top 5)

```mermaid
sequenceDiagram
    actor 고객
    participant Controller as ProductController
    participant Service as ProductService
    participant Repository as ProductRepository

    고객->>Controller: GET /api/v1/products/popular
    activate Controller

    Controller->>Service: getPopularProducts()
    activate Service

    Note over Service: 최근 3일 집계

    Service->>Repository: findPopularProducts(startDate, limit)
    activate Repository
    Note over Repository: SELECT oi.product_id, SUM(quantity)<br/>FROM order_items oi<br/>JOIN orders o ON oi.order_id = o.id<br/>WHERE o.created_at >= ?<br/>GROUP BY oi.product_id<br/>ORDER BY SUM(quantity) DESC<br/>LIMIT 5
    Repository-->>Service: List<PopularProductDto>
    deactivate Repository

    Service->>Service: 응답 변환
    Service-->>Controller: PopularProductsResponse
    deactivate Service

    Controller-->>고객: 200 OK
    deactivate Controller
```

**Related**: US-PROD-003, FR-STAT-001~003

---

## 2. 장바구니

### 2.1 장바구니 조회

```mermaid
sequenceDiagram
    actor 고객
    participant Controller as CartController
    participant Service as CartService
    participant Repository as CartItemRepository

    고객->>Controller: GET /api/v1/cart?userId=1
    activate Controller

    Controller->>Service: getCart(userId)
    activate Service

    Service->>Repository: findByUserId(userId)
    activate Repository
    Repository-->>Service: List<CartItem>
    deactivate Repository

    Service->>Service: 총액 계산 및 응답 변환
    Service-->>Controller: CartResponse
    deactivate Service

    Controller-->>고객: 200 OK
    deactivate Controller
```

**Related**: US-CART-002

---

### 2.2 장바구니에 상품 추가

```mermaid
sequenceDiagram
    actor 고객
    participant Controller as CartController
    participant Service as CartService
    participant CartRepo as CartItemRepository
    participant ProductRepo as ProductRepository

    고객->>Controller: POST /api/v1/cart/items<br/>{userId, productId, quantity}
    activate Controller

    Controller->>Service: addCartItem(request)
    activate Service

    Note over Service: 1. 상품 존재 & 재고 확인
    Service->>ProductRepo: findById(productId)
    activate ProductRepo

    alt 상품 없음
        ProductRepo-->>Service: Empty
        Service-->>Controller: throw ProductNotFoundException
        Controller-->>고객: 404 Not Found
    else 재고 부족
        ProductRepo-->>Service: Product (stock < quantity)
        Service-->>Controller: throw InsufficientStockException
        Controller-->>고객: 409 Conflict
    else 정상
        ProductRepo-->>Service: Product
        deactivate ProductRepo

        Note over Service: 2. 장바구니 중복 체크
        Service->>CartRepo: findByUserIdAndProductId(userId, productId)
        activate CartRepo

        alt 이미 존재
            CartRepo-->>Service: CartItem
            Service->>Service: 수량 증가
            Service->>CartRepo: save(cartItem)
            Note over CartRepo: UPDATE cart_items SET quantity = ?
        else 신규
            CartRepo-->>Service: Empty
            Service->>Service: 신규 생성
            Service->>CartRepo: save(cartItem)
            Note over CartRepo: INSERT INTO cart_items
        end

        CartRepo-->>Service: CartItem
        deactivate CartRepo

        Service-->>Controller: CartItemResponse
        deactivate Service
        Controller-->>고객: 201 Created
        deactivate Controller
    end
```

**Related**: US-CART-001

---

## 3. 주문 및 결제

### 3.1 주문 생성 및 결제 (성공 플로우)

```mermaid
sequenceDiagram
    actor 고객
    participant API
    participant OrderService
    participant DB

    고객->>API: POST /orders<br/>{userId, couponId?}
    API->>OrderService: createOrder()

    Note over OrderService,DB: 트랜잭션 시작

    OrderService->>DB: 1. 장바구니 조회
    OrderService->>DB: 2. 재고 확인 (synchronized/ReentrantLock)
    OrderService->>DB: 3. 쿠폰 검증 (있는 경우)
    OrderService->>DB: 4. 잔액 확인

    OrderService->>DB: 5. 주문 생성 (orders)
    OrderService->>DB: 6. 주문 항목 저장 (order_items)
    OrderService->>DB: 7. 재고 차감 (products.stock)
    OrderService->>DB: 8. 잔액 차감 (users.balance)
    OrderService->>DB: 9. 쿠폰 사용 처리 (user_coupons)
    OrderService->>DB: 10. 장바구니 삭제

    Note over OrderService,DB: 트랜잭션 커밋

    DB-->>OrderService: 주문 완료
    OrderService-->>API: OrderResponse
    API-->>고객: 201 Created
```

**Related**: US-ORDR-001, US-PAY-003, US-PAY-004

---

### 3.2 주문 실패 시나리오

```mermaid
sequenceDiagram
    actor 고객
    participant API
    participant OrderService
    participant DB

    고객->>API: POST /orders
    API->>OrderService: createOrder()

    Note over OrderService,DB: 트랜잭션 시작

    alt 재고 부족
        OrderService->>DB: 재고 확인 (synchronized/ReentrantLock)
        DB-->>OrderService: stock < quantity
        Note over OrderService,DB: 트랜잭션 롤백
        OrderService-->>API: 409 Conflict
        API-->>고객: 재고 부족

    else 잔액 부족
        OrderService->>DB: 잔액 확인
        DB-->>OrderService: balance < amount
        Note over OrderService,DB: 트랜잭션 롤백
        OrderService-->>API: 400 Bad Request
        API-->>고객: 잔액 부족

    else 쿠폰 만료/사용됨
        OrderService->>DB: 쿠폰 검증
        DB-->>OrderService: 쿠폰 유효하지 않음
        Note over OrderService,DB: 트랜잭션 롤백
        OrderService-->>API: 400 Bad Request
        API-->>고객: 쿠폰 오류
    end
```

**Related**: US-ORDR-001, US-PAY-003

---

## 4. 쿠폰 발급

### 4.1 선착순 쿠폰 발급 (성공)

```mermaid
sequenceDiagram
    actor 고객
    participant CouponController
    participant CouponService
    participant CouponRepository

    고객->>CouponController: POST /coupons/{eventId}/issue<br/>{userId}
    CouponController->>CouponService: issueCoupon()

    Note over CouponService,CouponRepository: 트랜잭션 시작

    CouponService->>CouponRepository: 쿠폰 이벤트 조회 (synchronized/ReentrantLock)

    alt 쿠폰 소진
        CouponRepository-->>CouponService: issued_quantity >= total_quantity
        Note over CouponService,CouponRepository: 트랜잭션 롤백
        CouponService-->>CouponController: 409 Conflict
        CouponController-->>고객: 쿠폰 소진

    else 발급 가능
        CouponService->>CouponRepository: 중복 발급 확인<br/>(user_id, coupon_event_id)

        alt 이미 발급받음
            CouponRepository-->>CouponService: 중복 발급
            Note over CouponService,CouponRepository: 트랜잭션 롤백
            CouponService-->>CouponController: 400 Bad Request
            CouponController-->>고객: 중복 발급

        else 발급 진행
            CouponService->>CouponRepository: INSERT INTO user_coupons
            CouponService->>CouponRepository: UPDATE coupon_events<br/>SET issued_quantity = issued_quantity + 1

            Note over CouponService,CouponRepository: 트랜잭션 커밋

            CouponRepository-->>CouponService: 발급 완료
            CouponService-->>CouponController: UserCouponResponse
            CouponController-->>고객: 201 Created
        end
    end
```

**Related**: US-COUP-001, US-COUP-003

---

### 4.2 보유 쿠폰 조회

```mermaid
sequenceDiagram
    actor 고객
    participant CouponController
    participant CouponService
    participant CouponRepository

    고객->>CouponController: GET /coupons?userId=1
    CouponController->>CouponService: getUserCoupons(userId)
    CouponService->>CouponRepository: SELECT * FROM user_coupons<br/>WHERE user_id = 1<br/>ORDER BY issued_at DESC
    CouponRepository-->>CouponService: 쿠폰 목록
    CouponService->>CouponService: 사용 가능/만료/사용됨 구분
    CouponService-->>CouponController: UserCouponListResponse
    CouponController-->>고객: 200 OK
```

**Related**: US-COUP-002

---

## 5. 핵심 패턴 요약

### 5.1 동시성 제어 (synchronized/ReentrantLock)

```java
// 재고 차감 시
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

// 쿠폰 발급 시
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

// 잔액 차감 시
public synchronized void updateBalance(Long userId, long amount) {
    User user = userRepository.findById(userId);
    user.updateBalance(amount);
    userRepository.save(user);
}
```

### 5.2 트랜잭션 범위

**주문 생성 트랜잭션 (원자성 보장)**:
1. 장바구니 조회
2. 재고 확인 및 락 획득
3. 쿠폰 검증 (선택)
4. 잔액 확인 및 락 획득
5. 주문 생성
6. 주문 항목 저장
7. 재고 차감
8. 잔액 차감
9. 쿠폰 사용 처리
10. 장바구니 삭제

→ 모든 작업이 성공하면 커밋, 하나라도 실패하면 롤백

### 5.3 에러 처리 우선순위

1. **비즈니스 검증** (400 Bad Request)
   - 잘못된 파라미터
   - 쿠폰 만료/사용됨

2. **리소스 없음** (404 Not Found)
   - 상품/주문/쿠폰 없음

3. **리소스 충돌** (409 Conflict)
   - 재고 부족
   - 쿠폰 소진
   - 잔액 부족

---

## 다이어그램 범례

### 참여자
- **고객**: 실제 사용자 (Actor)
- **API**: RESTful API 엔드포인트
- **Service**: 비즈니스 로직 레이어
- **DB**: 데이터베이스

### 주요 표기
- `synchronized/ReentrantLock`: 애플리케이션 레벨 동시성 제어
- `트랜잭션 시작/커밋/롤백`: 원자성 보장
- `alt-else`: 조건 분기 (에러 처리)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2025-10-29 | 초기 버전 작성 |
| v2.0 | 2025-10-29 | 도메인별 핵심 플로우로 단순화 |
