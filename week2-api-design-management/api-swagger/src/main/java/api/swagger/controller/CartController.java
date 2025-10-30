package api.swagger.controller;

import api.swagger.api.CartApi;
import api.swagger.dto.cart.AddCartItemRequest;
import api.swagger.dto.cart.AddCartItemResponse;
import api.swagger.dto.cart.CartResponse;
import api.swagger.dto.cart.UpdateCartItemRequest;
import api.swagger.dto.cart.UpdateCartItemResponse;
import api.swagger.dto.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController implements CartApi {

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@RequestParam Long userId) {
        CartResponse data = new CartResponse(
                1L,
                List.of(
                        new CartResponse.CartItem(
                                1L,
                                1L,
                                "노트북",
                                1500000L,
                                2,
                                3000000L,
                                50
                        ),
                        new CartResponse.CartItem(
                                2L,
                                2L,
                                "마우스",
                                30000L,
                                1,
                                30000L,
                                100
                        )
                ),
                3030000L,
                2
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/items")
    @Override
    public ResponseEntity<ApiResponse<AddCartItemResponse>> addCartItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {
        AddCartItemResponse data = new AddCartItemResponse(
                1L,
                request.getProductId(),
                "노트북",
                1500000L,
                request.getQuantity(),
                1500000L * request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PatchMapping("/items/{cartItemId}")
    @Override
    public ResponseEntity<ApiResponse<UpdateCartItemResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UpdateCartItemResponse data = new UpdateCartItemResponse(
                cartItemId,
                1L,
                "노트북",
                1500000L,
                request.getQuantity(),
                1500000L * request.getQuantity()
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Override
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId) {
        return ResponseEntity.noContent().build();
    }
}
