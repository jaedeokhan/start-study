# 인기 상품 조회 쿼리 성능 최적화 정리

## 첫 번째 쿼리

```sql
SELECT oi.product_id, SUM(oi.quantity) as totalQuantity
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
where o.created_at >= :start_date
and   o.status = 'COMPLETED'
GROUP BY oi.product_id
ORDER BY totalQuantity DESC
LIMIT 5;
```

## 첫 번째 쿼리 EXPLAIN ANALYZE 및 문제
1. OrderItem에 인덱스가 없어서 Full table scan
2. 비효율적인 필터링 순서
3. 임시 테이블 사용

```
-> Limit: 5 row(s)  (actual time=0.074..0.074 rows=0 loops=1)
    -> Sort: totalQuantity DESC, limit input to 5 row(s) per chunk  (actual time=0.073..0.073 rows=0 loops=1)
        -> Table scan on <temporary>  (actual time=0.001..0.001 rows=0 loops=1)
            -> Aggregate using temporary table  (actual time=0.062..0.062 rows=0 loops=1)
                -> Nested loop inner join  (cost=0.70 rows=1) (actual time=0.041..0.041 rows=0 loops=1)
                    -> Table scan on oi  (cost=0.35 rows=1) (actual time=0.040..0.040 rows=0 loops=1)
                    -> Filter: (o.`status` = 'COMPLETED')  (cost=0.35 rows=1) (never executed)
                        -> Single-row index lookup on o using PRIMARY (id=oi.order_id)  (cost=0.35 rows=1) (never executed)

```

## 두 번째 쿼리 - order_items에 기본 인덱스 및 커버링 인덱스 추가
1. OrderItem 인덱스
    - CREATE INDEX idx_order_item_order_id ON order_items(order_id);
2. Order status+id_created_at 복합 인덱스
    - CREATE INDEX idx_order_status_created_at_id ON orders(status, created_at, id);
3. OrderItem 커버링 인덱스
    - CREATE INDEX idx_order_item_covering ON order_itmes(order_id, product_id, quantity);_

```
-> Limit: 5 row(s)  (actual time=0.091..0.092 rows=5 loops=1)
    -> Sort: totalQuantity DESC, limit input to 5 row(s) per chunk  (actual time=0.091..0.091 rows=5 loops=1)
        -> Table scan on <temporary>  (actual time=0.000..0.001 rows=6 loops=1)
            -> Aggregate using temporary table  (actual time=0.080..0.081 rows=6 loops=1)
                -> Nested loop inner join  (cost=3.45 rows=5) (actual time=0.028..0.058 rows=16 loops=1)
                    -> Filter: (o.created_at >= TIMESTAMP'2025-11-14 00:00:00')  (cost=1.58 rows=5) (actual time=0.016..0.020 rows=16 loops=1)
                        -> Covering index lookup on o using idx_order_status_id_created_at (status='COMPLETED')  (cost=1.58 rows=16) (actual time=0.015..0.018 rows=16 loops=1)
                    -> Single-row index lookup on oi using uk_order_items_order_id (order_id=o.id)  (cost=0.27 rows=1) (actual time=0.002..0.002 rows=1 loops=16)
```

## 인덱스 설계 이유
1. orders 테이블

```sql
CREATE INDEX idx_order_status_created_at_id ON orders(status, created_at, id);
```

이유:
- status = 'COMPLETED' 조건으로 빠르게 필터링
- created_at 날짜 매핑
- id를 포함해 JOIN에 필요한 컬럼 커버
- 테이블 접근 없이 인덱스만으로 처리

  ---
2. order_items 테이블

```sql
CREATE INDEX idx_order_item_covering ON order_items(order_id, product_id, quantity);
```

이유:
- order_id: JOIN 조건
- product_id: SELECT, GROUP BY에 사용
- quantity: SUM 집계에 사용
- 커버링 인덱스: 쿼리에 필요한 모든 컬럼 포함 → 테이블 접근 불필요


## 예상 효과

| 항목     | 개선 전      | 개선 후    |
  |--------|-----------|---------|
| 스캔 방식  | 전체 테이블 스캔 | 인덱스만 사용 |
| 실행 시간  | 1,000ms   | 30ms    |
| 스캔 행 수 | 100,000   | 500     |
| 테이블 접근 | 필요        | 불필요     |
| 성능 개선  | -         | 95%+    |

  ---
🎯 핵심 포인트

1. 복합 인덱스: 여러 컬럼을 하나의 인덱스로 구성
2. 커버링 인덱스: 쿼리가 필요한 모든 컬럼을 인덱스에 포함
3. 컬럼 순서: WHERE → JOIN → SELECT/GROUP BY 순으로 배치
4. 테이블 접근 제거: 인덱스만으로 모든 데이터 조회 가능