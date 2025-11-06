package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.payment.ChargeBalanceUseCase;
import com.ecommerce.application.usecase.payment.GetBalanceUseCase;
import com.ecommerce.presentation.api.PaymentApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.payment.BalanceResponse;
import com.ecommerce.presentation.dto.payment.ChargeBalanceRequest;
import com.ecommerce.presentation.dto.payment.ChargeBalanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {
    // ✅ UseCase 주입
    private final GetBalanceUseCase getBalanceUseCase;
    private final ChargeBalanceUseCase chargeBalanceUseCase;

    @GetMapping("/balance")
    @Override
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @RequestParam Long userId
    ) {
        BalanceResponse response = getBalanceUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/balance/charge")
    @Override
    public ResponseEntity<ApiResponse<ChargeBalanceResponse>> chargeBalance(
            @Valid @RequestBody ChargeBalanceRequest request
    ) {
        ChargeBalanceResponse response = chargeBalanceUseCase.execute(
            request.getUserId(),
            request.getAmount()
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
