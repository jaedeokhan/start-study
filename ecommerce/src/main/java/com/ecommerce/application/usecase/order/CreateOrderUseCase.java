package com.ecommerce.application.usecase.order;

import com.ecommerce.application.lock.MultiDistributedLock;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.cart.exception.CartErrorCode;
import com.ecommerce.domain.cart.exception.EmptyCartException;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import com.ecommerce.domain.point.exception.InsufficientPointException;
import com.ecommerce.domain.point.exception.PointErrorCode;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.exception.ProductErrorCode;
import com.ecommerce.domain.product.exception.ProductNotFoundException;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.redis.ProductRankingRepository;
import com.ecommerce.infrastructure.repository.*;
import com.ecommerce.presentation.dto.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-ORD-001: 주문 생성
 * 복잡한 비즈니스 트랜잭션:
 * 1. 장바구니 조회 및 검증
 * 2. 재고 차감 (동시성 제어 - 비관적 락)
 * 3. 쿠폰 적용
 * 4. 잔액 차감 (동시성 제어 - 낙관적 락)
 * 5. 주문 생성 및 장바구니 클리어
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ProductRankingRepository productRankingRepository;

    @MultiDistributedLock(keyProvider = "getOrderLockKeys(#userId)")
    @Transactional
    public OrderResponse execute(Long userId, Long userCouponId) {
        log.debug("주문 생성 시도: userId={}, userCouponId={}", userId, userCouponId);
        // 1. 장바구니 조회
        List<CartItem> cartItems = validateAndGetCartItems(userId);

        // 2. 재고 차감
        Map<Long, Product> productMap = new HashMap<>();
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(ProductErrorCode.PRODUCT_NOT_FOUND));

            product.decreaseStock(item.getQuantity());

            productMap.put(product.getId(), product);
        }

        // 3. 총 금액 계산
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
            userCoupon = userCouponRepository.findByIdOrThrow(userCouponId);

            couponEvent = couponEventRepository.findByIdOrThrow(userCoupon.getCouponEventId());

            // 쿠폰 사용 가능 여부 검증
            userCoupon.validateUsable();

            // 할인 금액 계산
            discountAmount = calculateDiscount(couponEvent, totalAmount);

            // 쿠폰 사용 처리
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        long finalAmount = totalAmount - discountAmount;

        // 6. 사용자 포인트 차감
        User user = userRepository.findByIdOrThrow(userId);

        if (!user.hasPoint(finalAmount)) {
            throw new InsufficientPointException(PointErrorCode.INSUFFICIENT_POINT);
        }

        user.usePoint(finalAmount);

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
            orderItems.add(orderItemRepository.save(orderItem));
        }

        // 9. 장바구니 클리어
        cartRepository.deleteByUserId(userId);

        // 10. 포인트 이력 저장
        PointHistory pointHistory = new PointHistory(
                null,
                userId,
                -finalAmount,
                TransactionType.USE,
                user.getPointBalance(),
                order.getId(),
                String.format("주문 결제: 주문번호 %d", order.getId())
        );
        pointHistoryRepository.save(pointHistory);

        // 11. 응답 생성
        OrderResponse response = OrderResponse.from(order, orderItems);

        // 12. @Async, 트랜잭션 NEW - Redis 랭킹 업데이트
        updateRanking(order.getId(), orderItems);

        return response;
    }

    private List<CartItem> validateAndGetCartItems(Long userId) {
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException(CartErrorCode.EMPTY_CART);
        }
        return cartItems;
    }

    private long calculateDiscount(CouponEvent couponEvent, long totalAmount) {
        return switch (couponEvent.getDiscountType()) {
            case AMOUNT -> couponEvent.getDiscountAmount();
            case RATE -> Math.min(
                    totalAmount * couponEvent.getDiscountRate() / 100,
                    couponEvent.getMaxDiscountAmount()
            );
        };
    }

    /**
     * 락 키 생성 메서드 (LockKeyProvider)
     */
    public List<String> getOrderLockKeys(Long userId) {
        List<String> keys = new ArrayList<>();

        // 장바구니 조회하여 상품별 재고 락
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        for (CartItem item : cartItems) {
            keys.add("product:stock:" + item.getProductId());
        }

        // 포인트 락
        keys.add("point:use:" + userId);

        return keys;
    }

//    @Async
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRanking(Long orderId, List<OrderItem> orderItems) {
        Map<Long, Integer> productQuantities = orderItems.stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum  // 같은 상품이 여러 번 있으면 합산
                ));

        try {
            productRankingRepository.incrementTodayRanking(productQuantities);
        } catch (Exception e) {
            log.error("랭킹 업데이트 실패 - orderId: {}", orderId, e);
        }
    }
}