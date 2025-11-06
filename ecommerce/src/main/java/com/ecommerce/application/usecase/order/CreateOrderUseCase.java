package com.ecommerce.application.usecase.order;

import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.domain.user.User;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.cart.exception.EmptyCartException;
import com.ecommerce.domain.product.exception.InsufficientStockException;
import com.ecommerce.domain.coupon.exception.CouponNotFoundException;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.domain.payment.exception.InsufficientPointException;
import com.ecommerce.infrastructure.repository.*;
import com.ecommerce.presentation.dto.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-ORD-001: 주문 생성
 * 복잡한 비즈니스 트랜잭션:
 * 1. 장바구니 조회 및 검증
 * 2. 재고 차감 (동시성 제어)
 * 3. 쿠폰 적용
 * 4. 잔액 차감 (동시성 제어)
 * 5. 주문 생성 및 장바구니 클리어
 */
@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;
    private final OrderRepository orderRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public OrderResponse execute(Long userId, Long userCouponId) {
        // 1. 장바구니 조회
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("장바구니가 비어있습니다.");
        }

        // 2. 상품 정보 조회
        List<Long> productIds = cartItems.stream()
            .map(CartItem::getProductId)
            .collect(Collectors.toList());
        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        // 3. 재고 확인 및 차감 (동시성 제어는 Repository에서)
        for (CartItem item : cartItems) {
            Product product = productMap.get(item.getProductId());
            if (!product.hasStock(item.getQuantity())) {
                throw new InsufficientStockException(
                    String.format("재고 부족: %s (요청: %d, 재고: %d)",
                        product.getName(), item.getQuantity(), product.getStock())
                );
            }
            productRepository.decreaseStock(product.getId(), item.getQuantity());
        }

        // 4. 총 금액 계산
        long totalAmount = cartItems.stream()
            .mapToLong(item -> {
                Product product = productMap.get(item.getProductId());
                return product.getPrice() * item.getQuantity();
            })
            .sum();

        // 5. 쿠폰 적용 (선택적)
        long discountAmount = 0;
        UserCoupon userCoupon = null;
        CouponEvent couponEvent = null;

        if (userCouponId != null) {
            userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없습니다: " + userCouponId));

            couponEvent = couponEventRepository.findById(userCoupon.getCouponEventId())
                .orElseThrow(() -> new CouponEventNotFoundException("쿠폰 이벤트를 찾을 수 없습니다"));

            // 쿠폰 사용 가능 여부 검증
            userCoupon.validateUsable();

            // 할인 금액 계산
            discountAmount = calculateDiscount(couponEvent, totalAmount);

            // 쿠폰 사용 처리
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        long finalAmount = totalAmount - discountAmount;

        // 6. 사용자 포인트 차감 (동시성 제어는 Repository에서)
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.hasPoint(finalAmount)) {
            throw new InsufficientPointException(
                String.format("포인트 부족: 필요 금액 %d원, 현재 포인트 %d원", finalAmount, user.getPointBalance())
            );
        }
        userRepository.usePoint(userId, finalAmount);

        // 7. 주문 생성
        Order order = new Order(null, userId, totalAmount, discountAmount, finalAmount, userCouponId);
        order = orderRepository.save(order);

        // 8. 주문 아이템 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            Product product = productMap.get(item.getProductId());
            OrderItem orderItem = new OrderItem(
                null,
                order.getId(),
                product.getId(),
                product.getName(),
                item.getQuantity(),
                product.getPrice()
            );
            orderItems.add(orderRepository.saveOrderItem(orderItem));
        }

        // 9. 장바구니 클리어
        cartRepository.deleteByUserId(userId);

        // 10. 포인트 이력 저장
        User updatedUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        PointHistory pointHistory = new PointHistory(
            null,
            userId,
            -finalAmount,  // 사용은 음수
            TransactionType.USE,
            updatedUser.getPointBalance(),
            order.getId(),
            String.format("주문 결제: 주문번호 %d", order.getId())
        );
        pointHistoryRepository.save(pointHistory);

        // 11. 응답 생성
        return OrderResponse.from(order, orderItems);
    }

    private long calculateDiscount(CouponEvent couponEvent, long totalAmount) {
        switch (couponEvent.getDiscountType()) {
            case AMOUNT:
                return couponEvent.getDiscountAmount();
            case RATE:
                long calculatedDiscount = totalAmount * couponEvent.getDiscountRate() / 100;
                int maxDiscount = couponEvent.getMaxDiscountAmount();
                return Math.min(calculatedDiscount, maxDiscount);
            default:
                return 0;
        }
    }
}
