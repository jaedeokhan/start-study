package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.cart.CartItem;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderItem;
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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("인기 상품 API 통합 테스트")
class PopularProductApiIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User testUser;
    private List<Product> products;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User(null, "테스트유저", 10_000_000L));

        // 상품 10개 생성
        products = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            products.add(productRepository.save(
                    new Product(null, "상품" + i, "설명" + i, 10000L * i, 1000)
            ));
        }
    }

    @Test
    @DisplayName("인기 상품 Top 5 조회")
    void getPopularProducts_Top5() throws Exception {
        // given: 상품별 주문 생성
        createOrders(products.get(0).getId(), 10, 5);  // 50개
        createOrders(products.get(1).getId(), 10, 4);  // 40개
        createOrders(products.get(2).getId(), 10, 3);  // 30개
        createOrders(products.get(3).getId(), 10, 2);  // 20개
        createOrders(products.get(4).getId(), 10, 1);  // 10개
        createOrders(products.get(5).getId(), 10, 1);  // 10개 (6위, 제외)

        // when & then
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products", hasSize(5)))
                .andExpect(jsonPath("$.data.products[0].name").value("상품1"))
                .andExpect(jsonPath("$.data.products[0].salesCount").value(50))
                .andExpect(jsonPath("$.data.products[1].salesCount").value(40))
                .andExpect(jsonPath("$.data.products[2].salesCount").value(30))
                .andExpect(jsonPath("$.data.products[3].salesCount").value(20));
    }

    @Test
    @DisplayName("주문이 없을 때 빈 리스트 반환")
    void getPopularProducts_NoOrders() throws Exception {
        mockMvc.perform(get("/api/v1/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products", hasSize(0)));
    }

    /**
     * quantity개씩 count번 주문 생성
     */
    private void createOrders(Long productId, int quantity, int count) throws Exception {
        createOrdersForUser(testUser.getId(), productId, quantity, count);
    }

    private void createOrdersForUser(Long userId, Long productId, int quantity, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            // 기존 장바구니 항목 삭제 (중복 키 에러 방지)
            cartRepository.deleteByUserId(userId);

            cartRepository.save(new CartItem(null, userId, productId, quantity));
            CreateOrderRequest request = new CreateOrderRequest(userId, null);
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }
}
