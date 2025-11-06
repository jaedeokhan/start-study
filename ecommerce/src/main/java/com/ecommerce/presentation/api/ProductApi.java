package com.ecommerce.presentation.api;

import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.common.ErrorResponse;
import com.ecommerce.presentation.dto.product.PopularProductResponse;
import com.ecommerce.presentation.dto.product.ProductListResponse;
import com.ecommerce.presentation.dto.product.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "상품 API", description = "상품 조회 관련 API")
public interface ProductApi {

    @Operation(summary = "상품 목록 조회", description = "전체 상품 목록을 페이지네이션하여 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ProductListResponse.class))
        )
    })
    ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable Long productId
    );

    @Operation(summary = "인기 상품 조회", description = "최근 3일간 판매량 기준 상위 5개 상품을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PopularProductResponse.class))
        )
    })
    ResponseEntity<ApiResponse<PopularProductResponse>> getPopularProducts();
}
