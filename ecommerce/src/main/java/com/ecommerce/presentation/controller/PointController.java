package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.point.ChargePointUseCase;
import com.ecommerce.application.usecase.point.GetPointHistoryUseCase;
import com.ecommerce.application.usecase.point.GetPointUseCase;
import com.ecommerce.presentation.api.PointApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.point.ChargePointRequest;
import com.ecommerce.presentation.dto.point.ChargePointResponse;
import com.ecommerce.presentation.dto.point.PointHistoryListResponse;
import com.ecommerce.presentation.dto.point.PointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 포인트 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequiredArgsConstructor
public class PointController implements PointApi {
    private final GetPointUseCase getPointUseCase;
    private final ChargePointUseCase chargePointUseCase;
    private final GetPointHistoryUseCase getPointHistoryUseCase;

    @Override
    public ResponseEntity<ApiResponse<PointResponse>> getPoint(Long userId) {
        PointResponse response = getPointUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<ChargePointResponse>> chargePoint(ChargePointRequest request) {
        ChargePointResponse response = chargePointUseCase.execute(
            request.userId(),
            request.amount()
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<PointHistoryListResponse>> getPointHistory(Long userId) {
        PointHistoryListResponse response = getPointHistoryUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
