package io.hhplus.tdd;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("특정 사용자의 포인트 조회")
    void getUserPoint_Success() {
        // given
        long userId = 1L;

        // when
        ResponseEntity<UserPoint> response = restTemplate.getForEntity(
                "/point/{usreId}",
                UserPoint.class,
                userId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().point()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("특정 사용자의 포인트 충전 내역 조회")
    void getPointHistories_Success() {
        // given
        long userId = 1L;

        // when
        ResponseEntity<List<PointHistory>> response = restTemplate.exchange(
                "/point/{userId}/histories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PointHistory>>() {},
                userId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_Success() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        // 충전 전 포인트 조회
        UserPoint beforePoint = restTemplate.getForObject("/point/{userId}", UserPoint.class, userId);

        // when
        ResponseEntity<UserPoint> response = restTemplate.exchange(
                "/point/{userId}/charge",
                HttpMethod.PATCH,
                new HttpEntity<>(chargeAmount),
                UserPoint.class,
                userId
        );
        UserPoint result = response.getBody();

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(beforePoint.point() + chargeAmount);

        // 내역 확인
        ResponseEntity<List<PointHistory>> historyResponse = restTemplate.exchange(
                "/point/" + userId + "/histories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PointHistory>>() {}
        );

        List<PointHistory> histories = historyResponse.getBody();
        assertThat(histories).isNotNull();
        assertThat(histories).anyMatch(h ->
                h.userId() == userId &&
                h.amount() == chargeAmount &&
                h.type() == TransactionType.CHARGE
        );
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_Success() {
        // given
        long userId = 1L;
        long chargeAmount = 5000L;
        long useAmount = 2000L;

        // 먼저 충전
        restTemplate.exchange(
                "/point/{userId}/charge",
                HttpMethod.PATCH,
                new HttpEntity<>(chargeAmount),
                UserPoint.class,
                userId
        );

        // 충전 후 포인트 조회
        UserPoint beforePoint = restTemplate.getForObject("/point/{userId}", UserPoint.class, userId);

        // when
        ResponseEntity<UserPoint> response = restTemplate.exchange(
                "/point/{userId}/use",
                HttpMethod.PATCH,
                new HttpEntity<>(useAmount),
                UserPoint.class,
                userId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().point()).isEqualTo(beforePoint.point() - useAmount);

        // 내역 확인
        ResponseEntity<List<PointHistory>> historyResponse = restTemplate.exchange(
                "/point/{userId}/histories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PointHistory>>() {},
                userId
        );

        List<PointHistory> histories = historyResponse.getBody();
        assertThat(histories).isNotNull();
        assertThat(histories).anyMatch(h ->
                h.userId() == userId &&
                h.amount() == useAmount &&
                h.type() == TransactionType.USE
        );
    }

    @Test
    @DisplayName("포인트 충전 시 최소 금액 검증 실패")
    void chargePoint_Fail_WhenAmountBelowMinimum() {
        // given
        long userId = 1L;
        long invalidAmount = -100L;

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{userId}/charge",
                HttpMethod.PATCH,
                new HttpEntity<>(invalidAmount),
                String.class,
                userId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("포인트 사용 시 잔고 부족으로 실패")
    void usePoint_Fail_WhenInsufficientBalance() {
        // given
        long userId = 1L;
        long useAmount = 999999L;

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{userId}/use",
                HttpMethod.PATCH,
                new HttpEntity<>(useAmount),
                String.class,
                userId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("포인트 충전 시 100원 단위 검증 실패")
    void chargePoint_Fail_WhenAmountNotInValidUnit() {
        // given
        long userId = 1L;
        long invalidAmount = 1050L; // 100원 단위가 아님

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/point/{userId}/charge",
                HttpMethod.PATCH,
                new HttpEntity<>(invalidAmount),
                String.class,
                userId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
