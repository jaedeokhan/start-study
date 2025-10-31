# 이커머스 서비스 플로우차트

## 목차
1. [전체 시스템 플로우](#1-전체-시스템-플로우)
2. [상품 조회 플로우](#2-상품-조회-플로우)
3. [장바구니 플로우](#3-장바구니-플로우)
4. [주문 및 결제 플로우](#4-주문-및-결제-플로우)
5. [쿠폰 시스템 플로우](#5-쿠폰-시스템-플로우)

---

## 1. 전체 시스템 플로우
생략략

## 2. 상품 조회 플로우

### 2.1 상품 목록 및 상세 조회

```mermaid
flowchart TD
    Start([상품 조회 시작]) --> Choice{조회 유형<br/>선택}

    Choice -->|전체 상품| ListAPI[GET /products]
    Choice -->|인기 상품| PopularAPI[GET /products/popular]
    Choice -->|상품 상세| DetailAPI[GET /products/:id]

    ListAPI --> ListDB[(products 테이블<br/>전체 조회)]
    ListDB --> Pagination{페이지네이션}
    Pagination --> ListResponse[상품 목록 반환]

    PopularAPI --> PopularDB[(최근 3일간<br/>판매 통계 조회)]
    PopularDB --> AggregateTop5[판매량 Top 5<br/>집계]
    AggregateTop5 --> PopularResponse[인기 상품 반환]

    DetailAPI --> CheckProduct{상품<br/>존재?}
    CheckProduct -->|없음| Error404[404 Not Found]
    CheckProduct -->|존재| DetailDB[(상품 상세 조회<br/>+ 실시간 재고)]
    DetailDB --> DetailResponse[상품 상세 반환]

    ListResponse --> End([조회 완료])
    PopularResponse --> End
    DetailResponse --> End
    Error404 --> End

    style Start fill:#e1f5e1
    style End fill:#e1f5e1
    style Error404 fill:#ffe1e1
```

**Related**: US-PROD-001, US-PROD-002, US-PROD-003

---

## 3. 장바구니 플로우

### 3.1 장바구니 관리 플로우

```mermaid
flowchart TD
    Start([장바구니 작업 시작]) --> Action{작업 선택}

    Action -->|조회| GetCartAPI[GET /cart?userId=:id]
    Action -->|추가| AddCartAPI[POST /cart/items]
    Action -->|수량 변경| UpdateCartAPI[PATCH /cart/items/:id]
    Action -->|삭제| DeleteCartAPI[DELETE /cart/items/:id]

    GetCartAPI --> GetCartDB[(장바구니<br/>조회)]
    GetCartDB --> CalculateTotal[총액 계산]
    CalculateTotal --> CartResponse[장바구니 정보 반환]

    AddCartAPI --> CheckProductExists{상품<br/>존재?}
    CheckProductExists -->|없음| Error404[404 Not Found]
    CheckProductExists -->|존재| CheckStock{재고<br/>충분?}
    CheckStock -->|부족| Error409[409 Conflict<br/>재고 부족]
    CheckStock -->|충분| CheckDuplicate{장바구니에<br/>이미 있음?}
    CheckDuplicate -->|있음| UpdateQuantityDB[수량 증가]
    CheckDuplicate -->|없음| InsertCartDB[신규 항목 추가]
    UpdateQuantityDB --> AddSuccess[201 Created]
    InsertCartDB --> AddSuccess

    UpdateCartAPI --> CheckCartItem{장바구니<br/>항목 존재?}
    CheckCartItem -->|없음| Error404Cart[404 Not Found]
    CheckCartItem -->|존재| CheckNewStock{변경 수량<br/>재고 체크}
    CheckNewStock -->|부족| Error409Stock[409 Conflict]
    CheckNewStock -->|충분| UpdateDB[수량 업데이트]
    UpdateDB --> UpdateSuccess[200 OK]

    DeleteCartAPI --> CheckDelete{항목<br/>존재?}
    CheckDelete -->|없음| Error404Del[404 Not Found]
    CheckDelete -->|존재| DeleteDB[항목 삭제]
    DeleteDB --> DeleteSuccess[204 No Content]

    CartResponse --> End([완료])
    AddSuccess --> End
    UpdateSuccess --> End
    DeleteSuccess --> End
    Error404 --> End
    Error404Cart --> End
    Error404Del --> End
    Error409 --> End
    Error409Stock --> End

    style Start fill:#e1f5e1
    style End fill:#e1f5e1
    style Error404 fill:#ffe1e1
    style Error404Cart fill:#ffe1e1
    style Error404Del fill:#ffe1e1
    style Error409 fill:#ffe1e1
    style Error409Stock fill:#ffe1e1
```

**Related**: US-CART-001, US-CART-002, US-CART-003, US-CART-004

---

## 4. 주문 및 결제 플로우

### 4.1 주문 생성 및 결제 처리 (상세 플로우)

```mermaid
flowchart TD
    Start([주문 요청]) --> OrderAPI[POST /orders<br/>userId, couponId?]

    OrderAPI --> TxStart{{트랜잭션 시작}}

    TxStart --> Step1[1. 장바구니 조회]
    Step1 --> CheckCart{장바구니<br/>비어있음?}
    CheckCart -->|비어있음| ErrorEmptyCart[400 Bad Request<br/>장바구니 비어있음]
    CheckCart -->|있음| Step2

    Step2[2. 재고 확인<br/>synchronized/ReentrantLock] --> CheckStock{모든 상품<br/>재고 충분?}
    CheckStock -->|부족| ErrorStock[409 Conflict<br/>재고 부족]
    CheckStock -->|충분| Step3

    Step3[3. 쿠폰 검증] --> HasCoupon{쿠폰<br/>사용?}
    HasCoupon -->|아니오| Step4
    HasCoupon -->|예| CheckCoupon{쿠폰<br/>유효?}
    CheckCoupon -->|만료| ErrorCouponExpired[400 Bad Request<br/>쿠폰 만료]
    CheckCoupon -->|사용됨| ErrorCouponUsed[400 Bad Request<br/>쿠폰 이미 사용]
    CheckCoupon -->|유효| CalculateDiscount[할인 금액 계산]
    CalculateDiscount --> Step4

    Step4[4. 잔액 확인<br/>synchronized/ReentrantLock] --> CheckBalance{잔액<br/>충분?}
    CheckBalance -->|부족| ErrorBalance[400 Bad Request<br/>잔액 부족]
    CheckBalance -->|충분| Step5

    Step5[5. 주문 생성<br/>orders 테이블] --> Step6[6. 주문 항목 저장<br/>order_items 테이블]
    Step6 --> Step7[7. 재고 차감<br/>products.stock]
    Step7 --> Step8[8. 잔액 차감<br/>users.balance]
    Step8 --> Step9{쿠폰<br/>사용?}
    Step9 -->|예| Step10[9. 쿠폰 사용 처리<br/>user_coupons.is_used = true]
    Step9 -->|아니오| Step11
    Step10 --> Step11[10. 장바구니 삭제]

    Step11 --> TxCommit{{트랜잭션 커밋}}
    TxCommit --> Success[201 Created<br/>주문 완료]

    ErrorEmptyCart --> TxRollback{{트랜잭션 롤백}}
    ErrorStock --> TxRollback
    ErrorCouponExpired --> TxRollback
    ErrorCouponUsed --> TxRollback
    ErrorBalance --> TxRollback

    Success --> End([완료])
    TxRollback --> End

    style Start fill:#e1f5e1
    style Success fill:#e1f5e1
    style End fill:#e1f5e1
    style TxStart fill:#fff4e1
    style TxCommit fill:#fff4e1
    style TxRollback fill:#ffe1e1
    style ErrorEmptyCart fill:#ffe1e1
    style ErrorStock fill:#ffe1e1
    style ErrorCouponExpired fill:#ffe1e1
    style ErrorCouponUsed fill:#ffe1e1
    style ErrorBalance fill:#ffe1e1
```

**Related**: US-ORDR-001, US-PAY-003, US-PAY-004, NFR-INTG-004

---

### 4.2 잔액 충전 플로우

```mermaid
flowchart TD
    Start([잔액 충전 요청]) --> ChargeAPI[POST /balance/charge<br/>userId, amount]

    ChargeAPI --> ValidateAmount{충전 금액<br/>> 0?}
    ValidateAmount -->|아니오| ErrorAmount[400 Bad Request<br/>잘못된 금액]
    ValidateAmount -->|예| TxStart{{트랜잭션 시작}}

    TxStart --> LockUser[사용자 조회<br/>synchronized/ReentrantLock]
    LockUser --> CheckUser{사용자<br/>존재?}
    CheckUser -->|없음| ErrorUser[404 Not Found<br/>사용자 없음]
    CheckUser -->|존재| UpdateBalance[잔액 업데이트<br/>balance += amount]

    UpdateBalance --> TxCommit{{트랜잭션 커밋}}
    TxCommit --> Success[200 OK<br/>충전 완료]

    ErrorAmount --> End([완료])
    ErrorUser --> TxRollback{{트랜잭션 롤백}}
    TxRollback --> End
    Success --> End

    style Start fill:#e1f5e1
    style Success fill:#e1f5e1
    style End fill:#e1f5e1
    style TxStart fill:#fff4e1
    style TxCommit fill:#fff4e1
    style TxRollback fill:#ffe1e1
    style ErrorAmount fill:#ffe1e1
    style ErrorUser fill:#ffe1e1
```

**Related**: US-PAY-002

---

## 5. 쿠폰 시스템 플로우

### 5.1 선착순 쿠폰 발급 플로우

```mermaid
flowchart TD
    Start([쿠폰 발급 요청]) --> IssueAPI[POST /coupons/:eventId/issue<br/>userId]

    IssueAPI --> TxStart{{트랜잭션 시작}}

    TxStart --> LockEvent[쿠폰 이벤트 조회<br/>synchronized/ReentrantLock]
    LockEvent --> CheckEvent{이벤트<br/>존재?}
    CheckEvent -->|없음| ErrorEvent[404 Not Found<br/>이벤트 없음]
    CheckEvent -->|존재| CheckQuantity{쿠폰<br/>남아있음?}

    CheckQuantity -->|소진| ErrorSoldOut[409 Conflict<br/>쿠폰 소진]
    CheckQuantity -->|있음| CheckDuplicate{이미<br/>발급받음?}

    CheckDuplicate -->|예| ErrorDuplicate[409 Conflict<br/>중복 발급]
    CheckDuplicate -->|아니오| IssueCoupon[user_coupons 삽입]

    IssueCoupon --> IncreaseCount[issued_quantity += 1]
    IncreaseCount --> TxCommit{{트랜잭션 커밋}}
    TxCommit --> Success[201 Created<br/>쿠폰 발급 완료]

    ErrorEvent --> TxRollback{{트랜잭션 롤백}}
    ErrorSoldOut --> TxRollback
    ErrorDuplicate --> TxRollback
    TxRollback --> End([완료])
    Success --> End

    style Start fill:#e1f5e1
    style Success fill:#e1f5e1
    style End fill:#e1f5e1
    style TxStart fill:#fff4e1
    style TxCommit fill:#fff4e1
    style TxRollback fill:#ffe1e1
    style ErrorEvent fill:#ffe1e1
    style ErrorSoldOut fill:#ffe1e1
    style ErrorDuplicate fill:#ffe1e1
```

**Related**: US-COUP-001, US-COUP-003, NFR-INTG-002

---

### 5.2 쿠폰 조회 플로우

```mermaid
flowchart TD
    Start([쿠폰 조회 요청]) --> GetAPI[GET /coupons?userId=:id]

    GetAPI --> QueryDB[(user_coupons<br/>조회)]
    QueryDB --> FilterStatus{필터<br/>적용?}

    FilterStatus -->|전체| AllCoupons[전체 쿠폰]
    FilterStatus -->|AVAILABLE| Available[사용 가능<br/>쿠폰만]
    FilterStatus -->|USED| Used[사용된<br/>쿠폰만]
    FilterStatus -->|EXPIRED| Expired[만료된<br/>쿠폰만]

    AllCoupons --> Classify[쿠폰 분류<br/>사용 가능/만료/사용됨]
    Available --> Classify
    Used --> Classify
    Expired --> Classify

    Classify --> Summary[통계 계산<br/>총/사용 가능/사용/만료]
    Summary --> Response[200 OK<br/>쿠폰 목록 반환]
    Response --> End([완료])

    style Start fill:#e1f5e1
    style Response fill:#e1f5e1
    style End fill:#e1f5e1
```

**Related**: US-COUP-002

---

## 7. 플로우차트 범례

### 8.1 노드 유형

| 모양 | 의미 | 예시 |
|------|------|------|
| `([텍스트])` | 시작/종료 | 프로세스 시작/완료 |
| `[텍스트]` | 프로세스/작업 | API 호출, DB 조회 |
| `{텍스트}` | 의사결정 | 조건 분기 |
| `[(텍스트)]` | 데이터베이스 | DB 조회/저장 |
| `{{텍스트}}` | 트랜잭션 경계 | 시작/커밋/롤백 |

### 8.2 색상 규칙

| 색상 | 의미 |
|------|------|
| <span style="color: #e1f5e1">■</span> 연한 녹색 | 시작/성공/완료 |
| <span style="color: #ffe1e1">■</span> 연한 빨강 | 에러/실패 |
| <span style="color: #fff4e1">■</span> 연한 노랑 | 트랜잭션 경계 |
| <span style="color: #d4edda">■</span> 진한 녹색 | 커밋 완료 |

### 8.3 주요 패턴

#### 트랜잭션 패턴
```
{{트랜잭션 시작}} → 작업들 → {{트랜잭션 커밋}}
                    ↓
                 실패 시
                    ↓
            {{트랜잭션 롤백}}
```

#### 동시성 제어 패턴
```
FOR UPDATE → 락 획득 → 처리 → 커밋 (락 해제)
```

#### 에러 처리 패턴
```
검증 → 에러 발견 → 적절한 HTTP 상태 코드 반환
```

---

## 9. 핵심 비즈니스 플로우 요약

### 9.1 주요 플로우 체크리스트

| 플로우 | 트랜잭션 | 동시성 제어 | 핵심 검증 |
|--------|---------|-----------|---------|
| **상품 조회** | 불필요 | 불필요 | 상품 존재 여부 |
| **장바구니 추가** | 선택적 | 불필요 | 재고 확인, 중복 체크 |
| **주문 생성** | 필수 | synchronized/ReentrantLock (재고/잔액) | 재고/잔액/쿠폰 검증 |
| **잔액 충전** | 필수 | synchronized/ReentrantLock (잔액) | 금액 양수 체크 |
| **쿠폰 발급** | 필수 | synchronized/ReentrantLock (쿠폰 수량) | 쿠폰 수량, 중복 발급 |

### 9.2 트랜잭션 범위 정리

#### 주문 생성 트랜잭션 (가장 복잡)
1. 장바구니 조회
2. 재고 확인 (synchronized/ReentrantLock)
3. 쿠폰 검증 (선택)
4. 잔액 확인 (synchronized/ReentrantLock)
5. 주문 생성
6. 주문 항목 저장
7. 재고 차감
8. 잔액 차감
9. 쿠폰 사용 처리
10. 장바구니 삭제

→ **All or Nothing**: 모든 단계 성공 시 커밋, 하나라도 실패 시 롤백
