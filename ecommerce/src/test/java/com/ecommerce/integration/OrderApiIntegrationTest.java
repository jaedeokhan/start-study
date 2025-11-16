package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.cart.exception.CartErrorCode;
import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.DiscountType;
import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.domain.order.exception.OrderErrorCode;
import com.ecommerce.domain.point.exception.PointErrorCode;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.repository.*;
import com.ecommerce.presentation.dto.order.CreateOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Order API 통합 테스트")
class OrderApiIntegrationTest extends TestContainerConfig {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CouponEventRepository couponEventRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(new User(null, "테스트유저", 100000L));

        // 테스트 상품 생성
        testProduct1 = productRepository.save(new Product(null, "상품1", "상품1 상세", 10000L, 10));
        testProduct2 = productRepository.save(new Product(null, "상품2", "상품2 상세",20000L, 20));
    }

    @Test
    @DisplayName("주문 생성 - 쿠폰 미사용")
    void createOrder_WithoutCoupon() throws Exception {
        // given: 장바구니에 상품 추가
        cartRepository.save(new CartItem(null, testUser.getId(), testProduct1.getId(), 2));
        cartRepository.save(new CartItem(null, testUser.getId(), testProduct2.getId(), 1));

        CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), null);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.originalAmount").value(40000)) // 10000*2 + 20000*1
                .andExpect(jsonPath("$.data.discountAmount").value(0))
                .andExpect(jsonPath("$.data.finalAmount").value(40000))
                .andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    @DisplayName("주문 생성 - 쿠폰 사용")
    void createOrder_WithCoupon() throws Exception {
        // given: 쿠폰 이벤트 생성
        CouponEvent couponEvent = couponEventRepository.save(new CouponEvent(
                null, "5000원 할인 쿠폰", DiscountType.AMOUNT, 5000L, 100,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)
        ));

        // given: 사용자 쿠폰 발급
        UserCoupon userCoupon = userCouponRepository.save(new UserCoupon(
                null, testUser.getId(), couponEvent.getId(), LocalDateTime.now(), LocalDateTime.now().plusDays(7)
        ));

        // given: 장바구니에 상품 추가
        cartRepository.save(new CartItem(null, testUser.getId(), testProduct1.getId(), 3));

        CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), userCoupon.getId());

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.originalAmount").value(30000))
                .andExpect(jsonPath("$.data.discountAmount").value(5000))
                .andExpect(jsonPath("$.data.finalAmount").value(25000));
    }

    @Test
    @DisplayName("주문 생성 실패 - 빈 장바구니")
    void createOrder_EmptyCart() throws Exception {
        // given: 빈 장바구니
        CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), null);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CartErrorCode.EMPTY_CART.getCode()));
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_InsufficientStock() throws Exception {
        // given: 재고보다 많은 수량을 장바구니에 추가
        cartRepository.save(new CartItem(null, testUser.getId(), testProduct1.getId(), 100)); // 재고는 10개

        CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), null);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 포인트 부족")
    void createOrder_InsufficientPoint() throws Exception {
        // given: 포인트가 부족한 사용자
        User poorUser = userRepository.save(new User(null, "가난한유저",1000L));
        cartRepository.save(new CartItem(null, poorUser.getId(), testProduct1.getId(), 3)); // 30000원

        CreateOrderRequest request = new CreateOrderRequest(poorUser.getId(), null);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(PointErrorCode.INSUFFICIENT_POINT.getCode()));
    }

    @Test
    @DisplayName("주문 목록 조회")
    void getOrders() throws Exception {
        // given: 주문 생성을 위한 준비
        cartRepository.save(new CartItem(null, testUser.getId(), testProduct1.getId(), 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(testUser.getId(), null);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // when & then: 주문 목록 조회
        mockMvc.perform(get("/api/v1/orders")
                .param("userId", testUser.getId().toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orders", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.orders[0].orderId").exists())
                .andExpect(jsonPath("$.data.orders[0].finalAmount").exists())
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.size").value(10));
    }

    @Test
    @DisplayName("단일 주문 상세 조회")
    void getOrder() throws Exception {
        // given: 주문 생성
        cartRepository.save(new CartItem(null, testUser.getId(), testProduct1.getId(), 2));
        CreateOrderRequest createRequest = new CreateOrderRequest(testUser.getId(), null);

        String createResponse = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // orderId 추출 (간단하게 하기 위해 jsonPath 사용)
        Long orderId = objectMapper.readTree(createResponse)
                .path("data")
                .path("orderId")
                .asLong();

        // when & then: 주문 상세 조회
        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.originalAmount").value(20000))
                .andExpect(jsonPath("$.data.finalAmount").value(20000))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName").value("상품1"));
    }

    @Test
    @DisplayName("단일 주문 조회 실패 - 존재하지 않는 주문")
    void getOrder_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/orders/{orderId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(OrderErrorCode.ORDER_NOT_FOUND.getCode()));
    }
}
