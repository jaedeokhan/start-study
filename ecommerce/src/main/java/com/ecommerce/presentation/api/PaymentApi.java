package com.ecommerce.presentation.api;

import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.common.ErrorResponse;
import com.ecommerce.presentation.dto.payment.BalanceResponse;
import com.ecommerce.presentation.dto.payment.ChargeBalanceRequest;
import com.ecommerce.presentation.dto.payment.ChargeBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@Tag(name = "결제 API", description = "잔액 조회 및 충전 관련 API")
public interface PaymentApi {

    @Operation(
        summary = "잔액 조회",
        description = "사용자의 현재 잔액을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
        @Parameter(description = "사용자 ID", example = "1", required = true)
        @RequestParam Long userId
    );

    @Operation(
        summary = "잔액 충전",
        description = "사용자의 잔액을 충전합니다. 충전 금액은 0보다 커야 합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "충전 성공",
            content = @Content(schema = @Schema(implementation = ChargeBalanceResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (금액 0 이하)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<ApiResponse<ChargeBalanceResponse>> chargeBalance(
        @Parameter(description = "잔액 충전 요청 정보", required = true)
        @Valid @RequestBody ChargeBalanceRequest request
    );
}
