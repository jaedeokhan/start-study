package com.ecommerce.presentation.controller;

import com.ecommerce.presentation.api.PaymentApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.payment.BalanceResponse;
import com.ecommerce.presentation.dto.payment.ChargeBalanceRequest;
import com.ecommerce.presentation.dto.payment.ChargeBalanceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
public class PaymentController implements PaymentApi {

    @GetMapping("/balance")
    @Override
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @RequestParam Long userId
    ) {
        // Mock 데이터 반환
        BalanceResponse data = new BalanceResponse(
                userId,
                5000000L,
                LocalDateTime.of(2025, 10, 29, 14, 0, 0)
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/balance/charge")
    @Override
    public ResponseEntity<ApiResponse<ChargeBalanceResponse>> chargeBalance(
            @Valid @RequestBody ChargeBalanceRequest request
    ) {
        // Mock 데이터 반환
        Long previousBalance = 5000000L;
        Long chargedAmount = request.getAmount();
        Long currentBalance = previousBalance + chargedAmount;

        ChargeBalanceResponse data = new ChargeBalanceResponse(
                request.getUserId(),
                previousBalance,
                chargedAmount,
                currentBalance,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
