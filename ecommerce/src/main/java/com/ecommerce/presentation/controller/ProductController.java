package com.ecommerce.presentation.controller;

import com.ecommerce.application.usecase.product.GetPopularProductsUseCase;
import com.ecommerce.application.usecase.product.GetProductUseCase;
import com.ecommerce.application.usecase.product.GetProductsUseCase;
import com.ecommerce.presentation.api.ProductApi;
import com.ecommerce.presentation.dto.common.ApiResponse;
import com.ecommerce.presentation.dto.product.PopularProductResponse;
import com.ecommerce.presentation.dto.product.ProductListResponse;
import com.ecommerce.presentation.dto.product.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 API Controller
 * - UseCase를 통한 비즈니스 로직 실행
 */
@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {
    // ✅ UseCase 주입
    private final GetProductsUseCase getProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;

    @Override
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(int page, int size) {
        ProductListResponse response = getProductsUseCase.execute(page, size);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(Long productId) {
        ProductResponse response = getProductUseCase.execute(productId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Override
    public ResponseEntity<ApiResponse<PopularProductResponse>> getPopularProducts() {
        PopularProductResponse response = getPopularProductsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
