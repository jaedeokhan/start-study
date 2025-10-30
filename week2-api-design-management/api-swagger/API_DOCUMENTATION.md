# API 문서 자동화 가이드

이 프로젝트는 Spring Boot와 SpringDoc OpenAPI를 사용하여 API 문서를 자동 생성합니다.

## 프로젝트 구조

```
api-swagger/
├── src/main/java/api/swagger/
│   ├── api/                    # API 인터페이스 (Swagger 어노테이션 포함)
│   │   ├── ProductApi.java
│   │   ├── CartApi.java
│   │   ├── OrderApi.java
│   │   ├── PaymentApi.java
│   │   └── CouponApi.java
│   ├── controller/             # Controller 구현 (Mock 데이터 반환)
│   │   ├── ProductController.java
│   │   ├── CartController.java
│   │   ├── OrderController.java
│   │   ├── PaymentController.java
│   │   └── CouponController.java
│   ├── dto/                    # Request/Response DTO
│   │   ├── common/
│   │   ├── product/
│   │   ├── cart/
│   │   ├── order/
│   │   ├── payment/
│   │   └── coupon/
│   ├── config/
│   │   └── OpenApiConfig.java  # OpenAPI 설정
│   └── enums/
│       ├── DiscountType.java
│       └── CouponStatus.java
└── src/main/resources/
    ├── static/
    │   ├── index.html          # 정적 Swagger UI
    │   └── openapi.yaml        # 생성된 OpenAPI 명세서
    └── application.yml         # SpringDoc 설정

```

## 구현된 API 목록

### 1. 상품 API (`/api/v1/products`)
- `GET /products` - 상품 목록 조회 (페이지네이션)
- `GET /products/{productId}` - 상품 상세 조회
- `GET /products/popular` - 인기 상품 조회

### 2. 장바구니 API (`/api/v1/cart`)
- `GET /cart` - 장바구니 조회
- `POST /cart/items` - 장바구니에 상품 추가
- `PATCH /cart/items/{cartItemId}` - 장바구니 상품 수량 변경
- `DELETE /cart/items/{cartItemId}` - 장바구니 상품 삭제

### 3. 주문 API (`/api/v1/orders`)
- `POST /orders` - 주문 생성
- `GET /orders` - 주문 내역 조회 (페이지네이션)
- `GET /orders/{orderId}` - 주문 상세 조회

### 4. 결제 API (`/api/v1`)
- `GET /balance` - 잔액 조회
- `POST /balance/charge` - 잔액 충전

### 5. 쿠폰 API (`/api/v1`)
- `POST /coupons/{couponEventId}/issue` - 쿠폰 발급
- `GET /coupons` - 보유 쿠폰 조회
- `GET /coupon-events` - 쿠폰 이벤트 목록 조회

## 사용 방법

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 2. Swagger UI 접속

#### 동적 Swagger UI (SpringDoc 제공)
```
http://localhost:8080/swagger-ui.html
```

#### 정적 Swagger UI (정적 HTML)
```
http://localhost:8080/
```
또는
```
http://localhost:8080/index.html
```

### 3. OpenAPI 명세서 확인

#### YAML 형식
```
http://localhost:8080/v3/api-docs.yaml
```

#### JSON 형식
```
http://localhost:8080/v3/api-docs
```

## OpenAPI YAML 파일 재생성

애플리케이션을 수정한 후 OpenAPI YAML 파일을 다시 생성하려면:

### 방법 1: curl 사용

```bash
# 1. 애플리케이션 실행
./gradlew bootRun

# 2. 다른 터미널에서 YAML 다운로드
curl http://localhost:8080/v3/api-docs.yaml -o src/main/resources/static/openapi.yaml
```

### 방법 2: 브라우저 사용

1. 애플리케이션 실행: `./gradlew bootRun`
2. 브라우저에서 접속: `http://localhost:8080/v3/api-docs.yaml`
3. 파일을 `src/main/resources/static/openapi.yaml`로 저장

## 아키텍처 패턴

### API 인터페이스 분리 패턴

이 프로젝트는 **API 인터페이스 분리 패턴**을 사용합니다:

```java
// 1. API 인터페이스 (Swagger 어노테이션 포함)
@Tag(name = "상품 API", description = "상품 조회 관련 API")
public interface ProductApi {

    @Operation(summary = "상품 목록 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );
}

// 2. Controller (인터페이스 구현, 비즈니스 로직)
@RestController
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApi {

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
        int page, int size
    ) {
        // Mock 데이터 반환
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
```

### 장점

1. **관심사의 분리**: API 명세(인터페이스)와 구현(Controller)이 분리됨
2. **가독성 향상**: Controller 코드가 깔끔해지고 비즈니스 로직에 집중
3. **재사용성**: 동일한 인터페이스를 여러 구현체에서 사용 가능
4. **테스트 용이**: Mock 구현체를 쉽게 만들 수 있음
5. **문서 중앙화**: Swagger 어노테이션이 한 곳에 모여 있어 관리 용이

## 주요 설정

### application.yml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs          # OpenAPI JSON 경로
    enabled: true
  swagger-ui:
    path: /swagger-ui.html      # Swagger UI 경로
    enabled: true
    operationsSorter: alpha     # 작업 정렬 (알파벳순)
    tagsSorter: alpha           # 태그 정렬 (알파벳순)
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

### OpenApiConfig.java

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("이커머스 서비스 API")
                        .version("v1")
                        .description("API 설명"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버")
                ));
    }
}
```

## 정적 Swagger UI

`src/main/resources/static/index.html` 파일은 정적 Swagger UI를 제공합니다.

### 특징

- CDN을 통해 Swagger UI 라이브러리 로드
- `/openapi.yaml` 파일을 읽어서 API 문서 표시
- 애플리케이션 재시작 없이도 동작 (정적 파일)
- 운영 환경에서 동적 문서 생성 비활성화 가능

### 설정

```javascript
SwaggerUIBundle({
    url: "/openapi.yaml",           // OpenAPI YAML 파일 경로
    dom_id: '#swagger-ui',
    deepLinking: true,
    docExpansion: "list",           // 문서 확장 모드
    filter: true,                   // 필터 활성화
    tryItOutEnabled: true           // "Try it out" 활성화
});
```

## Mock 데이터

모든 Controller는 현재 Mock 데이터를 반환합니다. 실제 서비스 구현 시:

1. Service 레이어 추가
2. Repository 레이어 추가
3. Controller에서 Service 호출로 변경
4. Mock 데이터를 실제 데이터로 교체

## 추가 정보

### Swagger 어노테이션

- `@Tag`: API 그룹 정의
- `@Operation`: API 작업 설명
- `@ApiResponses`: 가능한 응답 코드와 설명
- `@Parameter`: 파라미터 설명
- `@Schema`: DTO 필드 설명

### 응답 구조

모든 API는 공통 응답 구조를 따릅니다:

```json
{
  "data": {
    // 실제 응답 데이터
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

에러 응답:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": "상세 정보 (optional)"
  },
  "timestamp": "2025-10-29T14:30:00"
}
```

## 참고 문서

- [SpringDoc OpenAPI 공식 문서](https://springdoc.org/)
- [Swagger UI 공식 문서](https://swagger.io/tools/swagger-ui/)
- [OpenAPI 3.0 스펙](https://swagger.io/specification/)
