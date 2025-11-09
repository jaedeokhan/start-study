package com.ecommerce.presentation.api;

import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.point.ChargePointRequest;
import com.ecommerce.presentation.dto.point.ChargePointResponse;
import com.ecommerce.presentation.dto.point.PointHistoryListResponse;
import com.ecommerce.presentation.dto.point.PointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Point", description = "포인트 관련 API")
@RequestMapping("/api/v1")
public interface PointApi {

    @Operation(summary = "포인트 조회", description = "사용자의 현재 포인트를 조회합니다")
    @GetMapping("/point")
    ResponseEntity<ApiResponse<PointResponse>> getPoint(
        @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId
    );

    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다")
    @PostMapping("/point/charge")
    ResponseEntity<ApiResponse<ChargePointResponse>> chargePoint(
        @Valid @RequestBody ChargePointRequest request
    );

    @Operation(summary = "포인트 이력 조회", description = "사용자의 포인트 이력을 조회합니다")
    @GetMapping("/point/history")
    ResponseEntity<ApiResponse<PointHistoryListResponse>> getPointHistory(
        @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId
    );
}
