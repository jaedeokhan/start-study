package com.ecommerce.presentation.api;

import com.ecommerce.presentation.dto.cart.AddCartItemRequest;
import com.ecommerce.presentation.dto.cart.AddCartItemResponse;
import com.ecommerce.presentation.dto.cart.CartResponse;
import com.ecommerce.presentation.dto.cart.UpdateCartItemRequest;
import com.ecommerce.presentation.dto.cart.UpdateCartItemResponse;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.common.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장바구니 API", description = "장바구니 관련 API")
@RequestMapping("/api/v1/cart")
public interface CartApi {

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니 내역을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CartResponse.class))
        )
    })
    @GetMapping
    ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId
    );

    @Operation(summary = "장바구니에 상품 추가", description = "장바구니에 상품을 추가하거나 기존 수량을 증가시킵니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "추가 성공",
            content = @Content(schema = @Schema(implementation = AddCartItemResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "재고 부족",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/items")
    ResponseEntity<ApiResponse<AddCartItemResponse>> addCartItem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "장바구니 상품 추가 요청",
                required = true
            )
            @Valid @RequestBody AddCartItemRequest request
    );

    @Operation(summary = "장바구니 상품 수량 변경", description = "장바구니에 담긴 상품의 수량을 변경합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "변경 성공",
            content = @Content(schema = @Schema(implementation = UpdateCartItemResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장바구니 항목을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "재고 부족",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PatchMapping("/items/{cartItemId}")
    ResponseEntity<ApiResponse<UpdateCartItemResponse>> updateCartItem(
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "장바구니 상품 수량 변경 요청",
                required = true
            )
            @Valid @RequestBody UpdateCartItemRequest request
    );

    @Operation(summary = "장바구니 상품 삭제", description = "장바구니에서 특정 상품을 제거합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "삭제 성공 (응답 본문 없음)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장바구니 항목을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/items/{cartItemId}")
    ResponseEntity<Void> deleteCartItem(
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId
    );
}
