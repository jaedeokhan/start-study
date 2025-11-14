# 이커머스 서비스 RESTful API 명세서

## 목차
1. [개요](#1-개요)
2. [공통 사항](#2-공통-사항)
3. [상품 API](#3-상품-api)
4. [장바구니 API](#4-장바구니-api)
5. [주문 API](#5-주문-api)
6. [포인트 API](#6-포인트-api)
7. [쿠폰 API](#7-쿠폰-api)
8. [에러 코드](#8-에러-코드)

---

## 1. 개요

### 1.1 Base URL
```
http://localhost:8080/api/v1
```

### 1.2 API 버전
- **현재 버전**: v1
- **버전 관리**: URL 경로에 버전 포함 (`/api/v1`)

### 1.3 인증
- 현재 버전에서는 인증/인가를 사용하지 않음
- userId는 요청 파라미터 또는 바디로 전달

---

## 2. 공통 사항

### 2.1 Content-Type
- **요청**: `application/json`
- **응답**: `application/json`

### 2.2 날짜/시간 형식
- **ISO 8601 형식**: `YYYY-MM-DDTHH:mm:ss`
- **예시**: `2025-10-29T14:30:00`

### 2.3 공통 응답 구조

#### 성공 응답
```json
{
  "data": {
    // 실제 응답 데이터
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

#### 에러 응답
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "사용자 친화적 에러 메시지",
    "details": "상세 에러 정보 (optional)"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

### 2.4 HTTP 상태 코드

| 상태 코드 | 설명 |
|----------|------|
| 200 OK | 요청 성공 |
| 201 Created | 리소스 생성 성공 |
| 204 No Content | 요청 성공, 응답 본문 없음 |
| 400 Bad Request | 잘못된 요청 |
| 404 Not Found | 리소스를 찾을 수 없음 |
| 409 Conflict | 리소스 충돌 (재고 부족, 쿠폰 소진 등) |
| 500 Internal Server Error | 서버 내부 오류 |

---

## 3. 상품 API

### 3.1 상품 목록 조회

**엔드포인트**: `GET /products`

**설명**: 전체 상품 목록을 조회합니다.

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| page | integer | N | 페이지 번호 (0부터 시작) | 0 |
| size | integer | N | 페이지 크기 | 20 |

**요청 예시**:
```http
GET /api/v1/products?page=0&size=20
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "products": [
      {
        "id": 1,
        "name": "노트북",
        "description": "고성능 노트북",
        "price": 1500000,
        "stock": 50
      },
      {
        "id": 2,
        "name": "마우스",
        "description": "무선 마우스",
        "price": 30000,
        "stock": 0
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 5,
      "totalElements": 100,
      "size": 20
    }
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PROD-001

---

### 3.2 상품 상세 조회

**엔드포인트**: `GET /products/{productId}`

**설명**: 특정 상품의 상세 정보를 조회합니다.

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| productId | long | Y | 상품 ID |

**요청 예시**:
```http
GET /api/v1/products/1
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "id": 1,
    "name": "노트북",
    "description": "고성능 노트북, 16GB RAM, 512GB SSD",
    "price": 1500000,
    "stock": 50,
    "createdAt": "2025-10-01T10:00:00",
    "updatedAt": "2025-10-29T14:00:00"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답** (404 Not Found):
```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "상품을 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PROD-002

---

### 3.3 인기 상품 조회

**엔드포인트**: `GET /products/popular`

**설명**: 최근 3일간 판매량 기준 상위 5개 상품을 조회합니다.

**요청 예시**:
```http
GET /api/v1/products/popular
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "products": [
      {
        "id": 1,
        "name": "노트북",
        "price": 1500000,
        "stock": 50,
        "salesCount": 150,
        "salesPeriod": {
          "startDate": "2025-10-26T00:00:00",
          "endDate": "2025-10-29T00:00:00"
        }
      },
      {
        "id": 5,
        "name": "키보드",
        "price": 80000,
        "stock": 30,
        "salesCount": 120,
        "salesPeriod": {
          "startDate": "2025-10-26T00:00:00",
          "endDate": "2025-10-29T00:00:00"
        }
      }
    ]
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PROD-003

---

## 4. 장바구니 API

### 4.1 장바구니 조회

**엔드포인트**: `GET /cart`

**설명**: 사용자의 장바구니 내역을 조회합니다.

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | long | Y | 사용자 ID |

**요청 예시**:
```http
GET /api/v1/cart?userId=1
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "userId": 1,
    "items": [
      {
        "cartItemId": 1,
        "productId": 1,
        "productName": "노트북",
        "price": 1500000,
        "quantity": 2,
        "subtotal": 3000000,
        "stock": 50
      },
      {
        "cartItemId": 2,
        "productId": 2,
        "productName": "마우스",
        "price": 30000,
        "quantity": 1,
        "subtotal": 30000,
        "stock": 100
      }
    ],
    "totalAmount": 3030000,
    "totalItems": 2
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-CART-002

---

### 4.2 장바구니에 상품 추가

**엔드포인트**: `POST /cart/items`

**설명**: 장바구니에 상품을 추가하거나 기존 수량을 증가시킵니다.

**요청 바디**:
```json
{
  "userId": 1,
  "productId": 1,
  "quantity": 2
}
```

**요청 필드**:
| 필드 | 타입 | 필수 | 제약 조건 | 설명 |
|------|------|------|----------|------|
| userId | long | Y | > 0 | 사용자 ID |
| productId | long | Y | > 0 | 상품 ID |
| quantity | integer | Y | >= 1 | 수량 |

**응답 예시** (201 Created):
```json
{
  "data": {
    "cartItemId": 1,
    "productId": 1,
    "productName": "노트북",
    "price": 1500000,
    "quantity": 2,
    "subtotal": 3000000
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답**:

404 Not Found - 상품 없음:
```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "상품을 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

409 Conflict - 재고 부족:
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

**Related**: US-CART-001

---

### 4.3 장바구니 상품 수량 변경

**엔드포인트**: `PATCH /cart/items/{cartItemId}`

**설명**: 장바구니에 담긴 상품의 수량을 변경합니다.

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cartItemId | long | Y | 장바구니 항목 ID |

**요청 바디**:
```json
{
  "quantity": 5
}
```

**요청 필드**:
| 필드 | 타입 | 필수 | 제약 조건 | 설명 |
|------|------|------|----------|------|
| quantity | integer | Y | >= 1 | 변경할 수량 |

**응답 예시** (200 OK):
```json
{
  "data": {
    "cartItemId": 1,
    "productId": 1,
    "productName": "노트북",
    "price": 1500000,
    "quantity": 5,
    "subtotal": 7500000
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답**:

404 Not Found:
```json
{
  "error": {
    "code": "CART_ITEM_NOT_FOUND",
    "message": "장바구니 항목을 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

409 Conflict - 재고 부족:
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

**Related**: US-CART-003

---

### 4.4 장바구니 상품 삭제

**엔드포인트**: `DELETE /cart/items/{cartItemId}`

**설명**: 장바구니에서 특정 상품을 제거합니다.

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cartItemId | long | Y | 장바구니 항목 ID |

**요청 예시**:
```http
DELETE /api/v1/cart/items/1
```

**응답 예시** (204 No Content):
```
(응답 본문 없음)
```

**에러 응답** (404 Not Found):
```json
{
  "error": {
    "code": "CART_ITEM_NOT_FOUND",
    "message": "장바구니 항목을 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-CART-004

---

## 5. 주문 API

### 5.1 주문 생성

**엔드포인트**: `POST /orders`

**설명**: 장바구니의 상품으로 주문을 생성하고 결제를 처리합니다.

**요청 바디**:
```json
{
  "userId": 1,
  "couponId": 5
}
```

**요청 필드**:
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | long | Y | 사용자 ID |
| couponId | long | N | 쿠폰 ID (없으면 할인 미적용) |

**응답 예시** (201 Created):
```json
{
  "data": {
    "orderId": 12345,
    "userId": 1,
    "status": "COMPLETED",
    "items": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "노트북",
        "price": 1500000,
        "quantity": 2,
        "subtotal": 3000000
      }
    ],
    "originalAmount": 3000000,
    "discountAmount": 100000,
    "finalAmount": 2900000,
    "couponUsed": {
      "couponId": 5,
      "couponName": "신규 가입 쿠폰",
      "discountAmount": 100000
    },
    "paymentInfo": {
      "paymentId": 678,
      "method": "BALANCE",
      "amount": 2900000,
      "paidAt": "2025-10-29T14:30:00"
    },
    "createdAt": "2025-10-29T14:30:00"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답**:

400 Bad Request - 장바구니 비어있음:
```json
{
  "error": {
    "code": "EMPTY_CART",
    "message": "장바구니가 비어있습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

409 Conflict - 재고 부족:
```json
{
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "재고가 부족합니다.",
    "details": "상품: 노트북 (ID: 1), 요청: 10, 재고: 5"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

400 Bad Request - 포인트 부족:
```json
{
  "error": {
    "code": "INSUFFICIENT_POINT",
    "message": "포인트가 부족합니다.",
    "details": "필요 금액: 2900000원, 현재 포인트: 1000000원"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

404 Not Found - 쿠폰 없음:
```json
{
  "error": {
    "code": "COUPON_NOT_FOUND",
    "message": "쿠폰을 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

400 Bad Request - 쿠폰 이미 사용됨:
```json
{
  "error": {
    "code": "COUPON_ALREADY_USED",
    "message": "이미 사용된 쿠폰입니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

400 Bad Request - 쿠폰 만료:
```json
{
  "error": {
    "code": "COUPON_EXPIRED",
    "message": "쿠폰의 유효기간이 만료되었습니다.",
    "details": "만료일: 2025-10-20T23:59:59"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-ORDR-001, US-PAY-003, US-PAY-004

---

### 5.2 주문 내역 조회

**엔드포인트**: `GET /orders`

**설명**: 사용자의 주문 내역을 조회합니다.

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| userId | long | Y | 사용자 ID | - |
| page | integer | N | 페이지 번호 | 0 |
| size | integer | N | 페이지 크기 | 10 |

**요청 예시**:
```http
GET /api/v1/orders?userId=1&page=0&size=10
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "orders": [
      {
        "orderId": 12345,
        "status": "COMPLETED",
        "originalAmount": 3000000,
        "discountAmount": 100000,
        "finalAmount": 2900000,
        "itemCount": 2,
        "createdAt": "2025-10-29T14:30:00"
      },
      {
        "orderId": 12344,
        "status": "COMPLETED",
        "originalAmount": 50000,
        "discountAmount": 0,
        "finalAmount": 50000,
        "itemCount": 1,
        "createdAt": "2025-10-28T10:15:00"
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 3,
      "totalElements": 25,
      "size": 10
    }
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-ORDR-002

---

### 5.3 주문 상세 조회

**엔드포인트**: `GET /orders/{orderId}`

**설명**: 특정 주문의 상세 정보를 조회합니다.

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| orderId | long | Y | 주문 ID |

**요청 예시**:
```http
GET /api/v1/orders/12345
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "orderId": 12345,
    "userId": 1,
    "status": "COMPLETED",
    "items": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "노트북",
        "price": 1500000,
        "quantity": 2,
        "subtotal": 3000000
      }
    ],
    "originalAmount": 3000000,
    "discountAmount": 100000,
    "finalAmount": 2900000,
    "couponUsed": {
      "couponId": 5,
      "couponName": "신규 가입 쿠폰",
      "discountAmount": 100000
    },
    "paymentInfo": {
      "paymentId": 678,
      "method": "BALANCE",
      "amount": 2900000,
      "paidAt": "2025-10-29T14:30:00"
    },
    "createdAt": "2025-10-29T14:30:00",
    "updatedAt": "2025-10-29T14:30:00"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답** (404 Not Found):
```json
{
  "error": {
    "code": "ORDER_NOT_FOUND",
    "message": "주문을 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-ORDR-002

---

## 6. 포인트 API

### 6.1 포인트 조회

**엔드포인트**: `GET /point`

**설명**: 사용자의 현재 포인트를 조회합니다.

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | long | Y | 사용자 ID |

**요청 예시**:
```http
GET /api/v1/point?userId=1
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "userId": 1,
    "pointBalance": 5000000,
    "lastUpdatedAt": "2025-10-29T14:00:00"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답** (404 Not Found):
```json
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PAY-001

---

### 6.2 포인트 충전

**엔드포인트**: `POST /point/charge`

**설명**: 사용자의 포인트를 충전합니다.

**요청 바디**:
```json
{
  "userId": 1,
  "amount": 1000000
}
```

**요청 필드**:
| 필드 | 타입 | 필수 | 제약 조건 | 설명 |
|------|------|------|----------|------|
| userId | long | Y | > 0 | 사용자 ID |
| amount | long | Y | > 0 | 충전 금액 |

**응답 예시** (200 OK):
```json
{
  "data": {
    "userId": 1,
    "previousBalance": 5000000,
    "chargedAmount": 1000000,
    "currentBalance": 6000000,
    "chargedAt": "2025-10-29T14:30:00"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PAY-002

---

### 6.3 포인트 이력 조회

**엔드포인트**: `GET /point/history`

**설명**: 사용자의 포인트 사용/충전 이력을 조회합니다.

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | long | Y | 사용자 ID |

**요청 예시**:
```http
GET /api/v1/point/history?userId=1
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "userId": 1,
    "histories": [
      {
        "id": 15,
        "userId": 1,
        "pointAmount": -2900000,
        "transactionType": "USE",
        "balanceAfter": 7100000,
        "orderId": 12345,
        "description": "주문 결제: 주문번호 12345",
        "createdAt": "2025-10-29T14:30:00"
      },
      {
        "id": 14,
        "userId": 1,
        "pointAmount": 1000000,
        "transactionType": "CHARGE",
        "balanceAfter": 10000000,
        "orderId": null,
        "description": "포인트 충전: 1000000원",
        "createdAt": "2025-10-29T10:00:00"
      }
    ],
    "totalCount": 15
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PAY-008

---

**에러 응답**:

400 Bad Request - 금액 0 이하:
```json
{
  "error": {
    "code": "INVALID_AMOUNT",
    "message": "충전 금액은 0보다 커야 합니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

404 Not Found - 사용자 없음:
```json
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-PAY-002

---

## 7. 쿠폰 API

### 7.1 쿠폰 발급

**엔드포인트**: `POST /coupons/{couponEventId}/issue`

**설명**: 선착순 쿠폰을 발급받습니다.

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| couponEventId | long | Y | 쿠폰 이벤트 ID |

**요청 바디**:
```json
{
  "userId": 1
}
```

**요청 필드**:
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | long | Y | 사용자 ID |

**응답 예시** (201 Created):
```json
{
  "data": {
    "couponId": 123,
    "couponEventId": 10,
    "userId": 1,
    "couponName": "신규 가입 쿠폰",
    "discountType": "AMOUNT",
    "discountAmount": 10000,
    "startDate": "2025-10-29T00:00:00",
    "endDate": "2025-11-30T23:59:59",
    "isUsed": false,
    "issuedAt": "2025-10-29T14:30:00"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**에러 응답**:

404 Not Found - 이벤트 없음:
```json
{
  "error": {
    "code": "COUPON_EVENT_NOT_FOUND",
    "message": "쿠폰 이벤트를 찾을 수 없습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

409 Conflict - 쿠폰 소진:
```json
{
  "error": {
    "code": "COUPON_SOLD_OUT",
    "message": "쿠폰이 모두 소진되었습니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

409 Conflict - 중복 발급:
```json
{
  "error": {
    "code": "COUPON_ALREADY_ISSUED",
    "message": "이미 발급받은 쿠폰입니다."
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-COUP-001, US-COUP-003

---

### 7.2 보유 쿠폰 조회

**엔드포인트**: `GET /coupons`

**설명**: 사용자가 보유한 쿠폰 목록을 조회합니다.

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| userId | long | Y | 사용자 ID | - |
| status | string | N | 쿠폰 상태 필터 (AVAILABLE, USED, EXPIRED) | - |

**요청 예시**:
```http
GET /api/v1/coupons?userId=1&status=AVAILABLE
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "coupons": [
      {
        "couponId": 123,
        "couponEventId": 10,
        "couponName": "신규 가입 쿠폰",
        "discountType": "AMOUNT",
        "discountAmount": 10000,
        "startDate": "2025-10-29T00:00:00",
        "endDate": "2025-11-30T23:59:59",
        "status": "AVAILABLE",
        "issuedAt": "2025-10-29T14:30:00",
        "usedAt": "2025-11-29T14:30:00",
        "expiresAt": "2025-12-29T14:30:00"
      },
      {
        "couponId": 124,
        "couponEventId": 11,
        "couponName": "첫 구매 10% 할인",
        "discountType": "RATE",
        "discountRate": 10,
        "maxDiscountAmount": 50000,
        "startDate": "2025-10-01T00:00:00",
        "endDate": "2025-10-31T23:59:59",
        "status": "AVAILABLE",
        "issuedAt": "2025-10-29T14:30:00",
        "usedAt": "2025-11-29T14:30:00",
        "expiresAt": "2025-12-29T14:30:00"      
      }
    ],
    "summary": {
      "totalCount": 5,
      "availableCount": 2,
      "usedCount": 2,
      "expiredCount": 1
    }
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

**Related**: US-COUP-002

---

### 7.3 쿠폰 이벤트 목록 조회

**엔드포인트**: `GET /coupon-events`

**설명**: 진행 중인 쿠폰 이벤트 목록을 조회합니다.

**Query Parameters**:

**요청 예시**:
```http
GET /api/v1/coupon-events
```

**응답 예시** (200 OK):
```json
{
  "data": {
    "events": [
      {
        "couponEventId": 10,
        "name": "신규 가입 쿠폰",
        "discountType": "AMOUNT",
        "discountAmount": 10000,
        "totalQuantity": 1000,
        "remainingQuantity": 450,
        "startDate": "2025-10-29T00:00:00",
        "endDate": "2025-11-30T23:59:59",
      }
    ]
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

---

## 8. 에러 코드

### 8.1 공통 에러 (1xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| INVALID_REQUEST | 400 | 잘못된 요청 형식 |
| INVALID_PARAMETER | 400 | 잘못된 파라미터 값 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 |
| DATABASE_ERROR | 500 | 데이터베이스 오류 |

### 8.2 상품 관련 (2xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| PRODUCT_NOT_FOUND | 404 | 상품을 찾을 수 없음 |
| INSUFFICIENT_STOCK | 409 | 재고 부족 |

### 8.3 장바구니 관련 (3xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| CART_ITEM_NOT_FOUND | 404 | 장바구니 항목을 찾을 수 없음 |
| EMPTY_CART | 400 | 장바구니가 비어있음 |
| INVALID_QUANTITY | 400 | 잘못된 수량 (0 이하) |

### 8.4 주문 관련 (4xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| ORDER_NOT_FOUND | 404 | 주문을 찾을 수 없음 |
| ORDER_CREATION_FAILED | 500 | 주문 생성 실패 |

### 8.5 포인트 관련 (5xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| INSUFFICIENT_POINT | 400 | 포인트 부족 |
| INVALID_AMOUNT | 400 | 잘못된 금액 (0 이하) |
| POINT_OPERATION_FAILED | 500 | 포인트 처리 실패 |
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없음 |

### 8.6 쿠폰 관련 (6xxx)

| 에러 코드 | HTTP 상태 | 설명 |
|----------|----------|------|
| COUPON_NOT_FOUND | 404 | 쿠폰을 찾을 수 없음 |
| COUPON_EVENT_NOT_FOUND | 404 | 쿠폰 이벤트를 찾을 수 없음 |
| COUPON_SOLD_OUT | 409 | 쿠폰 소진 |
| COUPON_ALREADY_ISSUED | 409 | 이미 발급받은 쿠폰 |
| COUPON_ALREADY_USED | 400 | 이미 사용된 쿠폰 |
| COUPON_EXPIRED | 400 | 쿠폰 유효기간 만료 |

---

## 9. 부록

### 9.1 Enum 타입 정의

#### OrderStatus (주문 상태)
```
- PENDING: 결제 대기
- COMPLETED: 결제 완료
- CANCELLED: 주문 취소
```

#### CouponStatus (쿠폰 상태)
```
- AVAILABLE: 사용 가능
- USED: 사용됨
- EXPIRED: 만료됨
```

#### DiscountType (할인 유형)
```
- AMOUNT: 고정 금액 할인
- RATE: 비율 할인
```

#### PaymentMethod (결제 수단)
```
- POINT: 포인트 결제
```

#### TransactionType (거래 유형)
```
- CHARGE: 포인트 충전
- USE: 포인트 사용
- REFUND: 포인트 환불
```

### 9.2 페이지네이션 공통 구조

```json
{
  "pagination": {
    "currentPage": 0,
    "totalPages": 10,
    "totalElements": 100,
    "size": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 9.3 타임스탬프 정책

- 모든 생성/수정 시간은 서버 시간 기준
- ISO 8601 형식 사용
- 타임존: UTC 또는 서버 로컬 타임 (일관성 유지)

### 9.4 동시성 제어 정책

다음 API는 동시성 제어가 적용됩니다:

1. **POST /orders**: 재고 차감 시 `synchronized` 또는 `ReentrantLock` 적용
2. **POST /coupons/{couponEventId}/issue**: 쿠폰 수량 차감 시 `synchronized` 또는 `ReentrantLock` 적용
3. **POST /point/charge**: 포인트 업데이트 시 `synchronized` 또는 `ReentrantLock` 적용

### 9.5 성능 고려사항

- 상품 목록 조회: 페이지네이션 필수 (기본 20개)
- 인기 상품 조회: 캐싱 권장 (TTL: 1시간)
- 주문 내역 조회: 페이지네이션 필수 (기본 10개)

### 9.6 API 호출 예시 (curl)

#### 상품 목록 조회
```bash
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=20" \
  -H "Content-Type: application/json"
```

#### 장바구니에 상품 추가
```bash
curl -X POST "http://localhost:8080/api/v1/cart/items" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }'
```

#### 주문 생성
```bash
curl -X POST "http://localhost:8080/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "couponId": 5
  }'
```

#### 쿠폰 발급
```bash
curl -X POST "http://localhost:8080/api/v1/coupons/10/issue" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1
  }'
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2025-10-29 | 초기 버전 작성 |
