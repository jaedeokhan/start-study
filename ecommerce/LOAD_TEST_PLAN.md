# E-Commerce 부하 테스트 계획서

## 1. 테스트 개요

### 1.1 목적
- 각 API의 성능 특성 파악 및 병목 지점 발견
- 동시 사용자 증가에 따른 응답 시간 변화 분석
- 선착순 쿠폰 발급의 정확성 및 동시성 제어 검증
- 적절한 서버 스펙 및 최적화 방안 도출
- 
### 1.2 테스트 환경
- **도구**: K6 (VM 환경)
- **APM**: Scouter(VM 환경)
- **WAS**: bootJar 구성(VM 환경)
  - Spring Boot 3.x (Java 17)
- **REDIS**: 컨테이너 환경
- **KRaft**: 컨테이너 환경

### 1.3 테스트 유형
본 테스트는 **Load Test (부하 테스트)** 로 진행합니다.
- **정의**: 점진적으로 사용자를 증가시키며 시스템의 성능을 측정
- **목표**: 정상 운영 범위 내에서의 성능 지표 수집

## 2. 테스트 대상 API

### 2.1 상품 다건 조회 (캐시 미적용)

#### API 정보
- **엔드포인트**: `GET /api/v1/products
- **설명**: 상품 목록 조회

#### 테스트 시나리오
- **부하 방법**: 5분 동안 가상 사용자 1,200명이 TPS 1,000 목표 요청

#### 성공 기준
| 지표        | 목표값         | 임계값        |
|-----------|-------------|------------|
| p95 응답시간  | < 1800ms    | < 1800ms   |
| p99 응답시간  | < 2000ms    | < 20000ms  |
| 에러율       | 0%          | < 1%       |

#### 예상 병목
- 캐시가 없기에 느릴 것으로 예상

#### 실행 결과
로컬 환경이라 캐시가 없어도 생각보다는 빠른 결과치가 나왔습니다. 
별도 구성이였다면 네트워크 시간도 있어서 시간이 더 걸리니 캐싱은 웬만하면 하는게 좋을 것 같습니다.

* 총 요청 건수: 277,708건
* 실패 건수: 0건
* p95: 1.41s
* p99: 1.78s
* TPS: 922

```
     execution: local
        script: 01-product-list.js
        output: -

     scenarios: (100.00%) 1 scenario, 1000 max VUs, 5m30s max duration (incl. graceful stop):
              * product_list: 1000.00 iterations/s for 5m0s (maxVUs: 20-1000, gracefulStop: 30s)
WARN[0127] Insufficient VUs, reached 1000 active VUs and cannot initialize more  executor=constant-arrival-rate scenario=product_list


  █ THRESHOLDS

    errors
    ✓ 'rate<0.001' rate=0.00%

    http_req_duration
    ✓ 'p(95)<1800' p(95)=1.41s
    ✓ 'p(99)<2000' p(99)=1.78s

    http_req_failed
    ✓ 'rate<0.001' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 277708  922.762858/s
    checks_succeeded...: 100.00% 277708 out of 277708
    checks_failed......: 0.00%   0 out of 277708

    ✓ status is 200

    CUSTOM
    errors.........................: 0.00%  0 out of 277708
    response_time..................: avg=695.564254 min=0.5161  med=875.5736 max=3240.1295 p(90)=1269.80512 p(95)=1411.445475

    HTTP
    http_req_duration..............: avg=695.56ms   min=516.1µs med=875.57ms max=3.24s     p(90)=1.26s      p(95)=1.41s
      { expected_response:true }...: avg=695.56ms   min=516.1µs med=875.57ms max=3.24s     p(90)=1.26s      p(95)=1.41s
    http_req_failed................: 0.00%  0 out of 277708
    http_reqs......................: 277708 922.762858/s

    EXECUTION
    dropped_iterations.............: 22293  74.074756/s
    iteration_duration.............: avg=695.82ms   min=516.1µs med=875.86ms max=3.24s     p(90)=1.27s      p(95)=1.41s
    iterations.....................: 277708 922.762858/s
    vus............................: 951    min=2           max=1000
    vus_max........................: 1000   min=28          max=1000

    NETWORK
    data_received..................: 319 MB 1.1 MB/s
    data_sent......................: 24 MB  78 kB/s

```


### 2.2 상품 단건 조회(로컬 캐시)

#### API 정보
- **엔드포인트**: `GET /api/v1/products/{productId}`
- **설명**: 특정 상품의 상세 정보 조회
- **파라미터**:
- `productId`: 현재는 1 고정, 향후 상품 ID (1-100 범위)

#### 테스트 시나리오
- **부하 방법**: 5분 동안 가상 사용자 2,500명이 TPS 2,000 목표 요청

#### 성공 기준
| 지표 | 목표값         | 임계값         |
|------|-------------|-------------|
| p95 응답시간 | < 100ms     | < 100ms     |
| p99 응답시간 | < 200ms     | < 200ms     |
| 에러율 | 0%          | < 1%        |

#### 예상 병목
- 단일 상품 조회는 캐시 조회로 가장 빠를 것으로 예상

#### 실행 결과
**상품 ID를 랜덤으로 출력을 해보는건 다음 기회에 하기로 하고 현재는 productId=1인 캐시에 존재하는 항목만 가져오게 테스트를 진행했습니다.**
당연하게 캐싱된 데이터를 바로 가져오니 정말 빠르게 가져왔습니다.

* 총 요청 건수: 599,919건
* 실패 건수: x
* p95: 660.59µs
* p99: 1.49ms
* TPS: 1999

```
     execution: local
        script: 02-product-detail.js
        output: -

     scenarios: (100.00%) 1 scenario, 2500 max VUs, 5m30s max duration (incl. graceful stop):
              * product_list: 2000.00 iterations/s for 5m0s (maxVUs: 20-2500, gracefulStop: 30s)


  █ THRESHOLDS

    errors
    ✓ 'rate<0.001' rate=0.00%

    http_req_duration
    ✓ 'p(95)<100' p(95)=660.59µs
    ✓ 'p(99)<200' p(99)=1.49ms

    http_req_failed
    ✓ 'rate<0.001' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 599919  1999.723338/s
    checks_succeeded...: 100.00% 599919 out of 599919
    checks_failed......: 0.00%   0 out of 599919

    ✓ status is 200

    CUSTOM
    errors.........................: 0.00%  0 out of 599919
    response_time..................: avg=0.18605  min=0  med=0  max=32.5231 p(90)=0.5549  p(95)=0.6606

    HTTP
    http_req_duration..............: avg=186.04µs min=0s med=0s max=32.52ms p(90)=554.9µs p(95)=660.59µs
      { expected_response:true }...: avg=186.04µs min=0s med=0s max=32.52ms p(90)=554.9µs p(95)=660.59µs
    http_req_failed................: 0.00%  0 out of 599919
    http_reqs......................: 599919 1999.723338/s

    EXECUTION
    dropped_iterations.............: 82     0.273332/s
    iteration_duration.............: avg=278.44µs min=0s med=0s max=70.88ms p(90)=598.7µs p(95)=920.2µs
    iterations.....................: 599919 1999.723338/s
    vus............................: 0      min=0           max=6
    vus_max........................: 48     min=35          max=48

    NETWORK
    data_received..................: 315 MB 1.0 MB/s
    data_sent......................: 52 MB  174 kB/s




running (5m00.0s), 0000/0048 VUs, 599919 complete and 0 interrupted iterations
product_list ✓ [=========================] 0000/0048 VUs  5m0s  2000.00 iters/s
```

---

### 2.3 인기 상품 조회(레디스 캐시)

#### API 정보
- **엔드포인트**: `GET /api/v1/products/popular`
- **설명**: 최근 3일간 판매량 기준 상위 5개 상품 조회
- **파라미터**: 없음

#### 테스트 시나리오
- **부하 방법**: 5분 동안 가상 사용자 2,500명이 TPS 2,000 목표 요청

#### 성공 기준
| 지표 | 목표값     | 임계값     |
|------|---------|---------|
| p95 응답시간 | < 100ms | < 200ms |
| p99 응답시간 | < 200ms | < 200ms |
| 에러율 | 0%      | < 1%    |

#### 예상 병목
- **집계 쿼리 성능**: GROUP BY, ORDER BY 사용

#### 실행 결과
레디스 캐시에 존재하는 인기 상품을 가져오기에 예상치 p95, p99에 넉넉하게 들어왔습니다.
레디스 캐시가 로컬 캐시보다는 p95, p99가 살짝 높은걸 확인 가능합니다.

* 총 요청 건수: 598,770건
* 실패 건수: x
* p95: 1.33ms
* p99: 4.79ms
* TPS: 1995

---

### 향후 도전과제... 2.4 선착순 쿠폰 발급 (1000개 한정)
### 향후 도전과제... 2.5 주문
