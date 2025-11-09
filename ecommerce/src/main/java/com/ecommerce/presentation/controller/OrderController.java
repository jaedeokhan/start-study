package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.order.CreateOrderUseCase;
import com.ecommerce.application.usecase.order.GetOrderUseCase;
import com.ecommerce.application.usecase.order.GetOrdersUseCase;
import com.ecommerce.presentation.api.OrderApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.order.CreateOrderRequest;
import com.ecommerce.presentation.dto.order.OrderListResponse;
import com.ecommerce.presentation.dto.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {
    // ✅ UseCase 주입
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;

    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(CreateOrderRequest request) {
        OrderResponse response = createOrderUseCase.execute(request.getUserId(), request.getCouponId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            Long userId,
            int page,
            int size
    ) {
        OrderListResponse response = getOrdersUseCase.execute(userId, page, size);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(Long orderId) {
        OrderResponse response = getOrderUseCase.execute(orderId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
