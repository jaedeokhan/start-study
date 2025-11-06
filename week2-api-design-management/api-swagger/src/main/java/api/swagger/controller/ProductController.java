package api.swagger.controller;

import api.swagger.api.ProductApi;
import api.swagger.dto.common.ApiResponse;
import api.swagger.dto.common.PaginationInfo;
import api.swagger.dto.product.PopularProductResponse;
import api.swagger.dto.product.ProductListResponse;
import api.swagger.dto.product.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApi {

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ProductListResponse data = new ProductListResponse(
                List.of(
                        new ProductListResponse.ProductSummary(1L, "노트북", "고성능 노트북", 1500000L, 50),
                        new ProductListResponse.ProductSummary(2L, "마우스", "무선 마우스", 30000L, 0)
                ),
                new PaginationInfo(0, 5, 100, 20)
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/{productId}")
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        ProductResponse data = new ProductResponse(
                1L,
                "노트북",
                "고성능 노트북, 16GB RAM, 512GB SSD",
                1500000L,
                50,
                LocalDateTime.of(2025, 10, 1, 10, 0),
                LocalDateTime.of(2025, 10, 29, 14, 0)
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/popular")
    @Override
    public ResponseEntity<ApiResponse<PopularProductResponse>> getPopularProducts() {
        PopularProductResponse data = new PopularProductResponse(
                List.of(
                        new PopularProductResponse.PopularProduct(
                                1L,
                                "노트북",
                                1500000L,
                                50,
                                150,
                                new PopularProductResponse.SalesPeriod(
                                        LocalDateTime.of(2025, 10, 26, 0, 0),
                                        LocalDateTime.of(2025, 10, 29, 0, 0)
                                )
                        ),
                        new PopularProductResponse.PopularProduct(
                                5L,
                                "키보드",
                                80000L,
                                30,
                                120,
                                new PopularProductResponse.SalesPeriod(
                                        LocalDateTime.of(2025, 10, 26, 0, 0),
                                        LocalDateTime.of(2025, 10, 29, 0, 0)
                                )
                        )
                )
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
