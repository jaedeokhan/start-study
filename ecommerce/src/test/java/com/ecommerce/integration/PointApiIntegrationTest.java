package com.ecommerce.integration;

import com.ecommerce.config.TestContainerConfig;
import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.domain.point.TransactionType;
import com.ecommerce.domain.user.User;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import com.ecommerce.infrastructure.repository.UserRepository;
import com.ecommerce.presentation.dto.point.ChargePointRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Point API 통합 테스트")
class PointApiIntegrationTest extends TestContainerConfig {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(new User(null, "테스트유저", 10000L));
    }

    @Test
    @DisplayName("포인트 조회")
    void getPoint() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/points/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.pointBalance").value(10000));
    }

    @Test
    @DisplayName("포인트 조회 실패 - 존재하지 않는 사용자")
    void getPoint_UserNotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/points/{userId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("포인트 충전")
    void chargePoint() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(testUser.getId(), 5000L);

        // when & then
        mockMvc.perform(post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.previousBalance").value(10000))
                .andExpect(jsonPath("$.data.chargedAmount").value(5000))
                .andExpect(jsonPath("$.data.currentBalance").value(15000));

        // 충전 후 포인트 잔액 확인
        mockMvc.perform(get("/api/v1/points/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointBalance").value(15000));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 음수 금액")
    void chargePoint_NegativeAmount() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(testUser.getId(), -1000L);

        // when & then
        mockMvc.perform(post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 존재하지 않는 사용자")
    void chargePoint_UserNotFound() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(999999L, 5000L);

        // when & then
        mockMvc.perform(post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("포인트 충전 이력 조회")
    void getPointHistory() throws Exception {
        // given: 포인트 충전 이력 생성
        pointHistoryRepository.save(new PointHistory(
                null, testUser.getId(), 5000L, TransactionType.CHARGE, 15000L, null, "테스트 충전"
        ));
        pointHistoryRepository.save(new PointHistory(
                null, testUser.getId(), 3000L, TransactionType.CHARGE, 18000L, null, "테스트 충전2"
        ));

        // when & then
        mockMvc.perform(get("/api/v1/points/{userId}/history", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.histories", hasSize(2)))
                .andExpect(jsonPath("$.data.histories[0].pointAmount").exists())
                .andExpect(jsonPath("$.data.histories[0].transactionType").exists())
                .andExpect(jsonPath("$.data.histories[0].balanceAfter").exists());
    }

    @Test
    @DisplayName("포인트 충전 이력 조회 - 이력 없음")
    void getPointHistory_Empty() throws Exception {
        // given: 새 사용자 (이력 없음)
        User newUser = userRepository.save(new User(null, "신규유저", 0L));

        // when & then
        mockMvc.perform(get("/api/v1/points/{userId}/history", newUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(newUser.getId()))
                .andExpect(jsonPath("$.data.histories", hasSize(0)));
    }
}
