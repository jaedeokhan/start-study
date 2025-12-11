package com.ecommerce.presentation.api;

import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.common.ErrorResponse;
import com.ecommerce.presentation.dto.coupon.CouponEventListResponse;
import com.ecommerce.presentation.dto.coupon.UserCouponListResponse;
import com.ecommerce.presentation.dto.coupon.IssueCouponResponse;
import com.ecommerce.presentation.dto.coupon.IssueCouponRequest;
import com.ecommerce.domain.coupon.CouponStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "쿠폰 API", description = "쿠폰 발급 및 조회 관련 API")
@RequestMapping("/api/v1")
public interface CouponApi {

    @Operation(
            summary = "쿠폰 발급",
            description = "선착순 쿠폰을 발급받습니다. 이미 발급받은 쿠폰이거나 수량이 소진된 경우 발급이 불가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "쿠폰 발급 성공",
                    content = @Content(schema = @Schema(implementation = IssueCouponResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "쿠폰 이벤트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "쿠폰 소진 또는 중복 발급",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/coupons/{couponEventId}/issue")
    ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            @Parameter(description = "쿠폰 이벤트 ID", example = "10", required = true)
            @PathVariable Long couponEventId,
            @Parameter(description = "쿠폰 발급 요청", required = true)
            @Valid @RequestBody IssueCouponRequest request
    );

    @Operation(
            summary = "비동기 쿠폰 발급",
            description = "선착순 쿠폰을 발급받습니다. 이미 발급받은 쿠폰이거나 수량이 소진된 경우 발급이 불가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "쿠폰 발급 성공",
                    content = @Content(schema = @Schema(implementation = IssueCouponResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "쿠폰 이벤트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "쿠폰 소진 또는 중복 발급",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/coupons/{couponEventId}/asyncIssue")
    ResponseEntity<ApiResponse<IssueCouponResponse>> asyncIssueCoupon(
            @Parameter(description = "쿠폰 이벤트 ID", example = "10", required = true)
            @PathVariable Long couponEventId,
            @Parameter(description = "쿠폰 발급 요청", required = true)
            @Valid @RequestBody IssueCouponRequest request
    );

    @Operation(
            summary = "보유 쿠폰 조회",
            description = "사용자가 보유한 쿠폰 목록을 조회합니다. 상태별 필터링이 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserCouponListResponse.class))
            )
    })
    @GetMapping("/coupons")
    ResponseEntity<ApiResponse<UserCouponListResponse>> getCoupons(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,
            @Parameter(
                    description = "쿠폰 상태 필터 (AVAILABLE: 사용 가능, USED: 사용됨, EXPIRED: 만료됨)",
                    example = "AVAILABLE"
            )
            @RequestParam(required = false) CouponStatus status
    );

    @Operation(
            summary = "쿠폰 이벤트 목록 조회",
            description = "진행 중인 쿠폰 이벤트 목록을 조회합니다. 상태별 필터링이 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CouponEventListResponse.class))
            )
    })
    @GetMapping("/coupons-events")
    ResponseEntity<ApiResponse<CouponEventListResponse>> getCouponEvents(
    );
}
