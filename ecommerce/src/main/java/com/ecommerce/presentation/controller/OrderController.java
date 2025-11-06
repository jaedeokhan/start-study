package com.ecommerce.presentation.controller;

import com.ecommerce.presentation.api.OrderApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.common.PaginationInfo;
import com.ecommerce.presentation.dto.order.CreateOrderRequest;
import com.ecommerce.presentation.dto.order.OrderListResponse;
import com.ecommerce.presentation.dto.order.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderApi {

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody CreateOrderRequest request) {
        // Mock 주문 생성 응답
        OrderResponse data = new OrderResponse(
                12345L,
                request.getUserId(),
                "COMPLETED",
                List.of(
                        new OrderResponse.OrderItem(
                                1L,
                                1L,
                                "노트북",
                                1500000L,
                                2,
                                3000000L
                        )
                ),
                3000000L,
                100000L,
                2900000L,
                request.getCouponId() != null ? new OrderResponse.CouponUsed(
                        request.getCouponId(),
                        "신규 가입 쿠폰",
                        100000L
                ) : null,
                new OrderResponse.PaymentInfo(
                        678L,
                        "BALANCE",
                        2900000L,
                        LocalDateTime.of(2025, 10, 29, 14, 30, 0)
                ),
                LocalDateTime.of(2025, 10, 29, 14, 30, 0),
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Mock 주문 목록 응답
        OrderListResponse data = new OrderListResponse(
                List.of(
                        new OrderListResponse.OrderSummary(
                                12345L,
                                "COMPLETED",
                                3000000L,
                                100000L,
                                2900000L,
                                2,
                                LocalDateTime.of(2025, 10, 29, 14, 30, 0)
                        ),
                        new OrderListResponse.OrderSummary(
                                12344L,
                                "COMPLETED",
                                50000L,
                                0L,
                                50000L,
                                1,
                                LocalDateTime.of(2025, 10, 28, 10, 15, 0)
                        )
                ),
                new PaginationInfo(0, 3, 25, 10)
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/{orderId}")
    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        // Mock 주문 상세 응답
        OrderResponse data = new OrderResponse(
                orderId,
                1L,
                "COMPLETED",
                List.of(
                        new OrderResponse.OrderItem(
                                1L,
                                1L,
                                "노트북",
                                1500000L,
                                2,
                                3000000L
                        )
                ),
                3000000L,
                100000L,
                2900000L,
                new OrderResponse.CouponUsed(
                        5L,
                        "신규 가입 쿠폰",
                        100000L
                ),
                new OrderResponse.PaymentInfo(
                        678L,
                        "BALANCE",
                        2900000L,
                        LocalDateTime.of(2025, 10, 29, 14, 30, 0)
                ),
                LocalDateTime.of(2025, 10, 29, 14, 30, 0),
                LocalDateTime.of(2025, 10, 29, 14, 30, 0)
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
