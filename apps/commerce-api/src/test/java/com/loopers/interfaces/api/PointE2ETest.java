package com.loopers.interfaces.api;

import com.loopers.domain.example.Gender;
import com.loopers.domain.example.Point;
import com.loopers.domain.example.PointService;
import com.loopers.domain.example.User;
import com.loopers.domain.example.UserService;
import com.loopers.infrastructure.example.PointRepository;
import com.loopers.infrastructure.example.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PointE2ETest {

    private static final String ENDPOINT_GET_POINTS = "/api/points";
    private static final String ENDPOINT_CHARGE_POINTS = "/api/points/charge";
    private static final String ENDPOINT_USE_POINTS = "/api/points/use";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockitoBean
    private PointRepository pointRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PointService pointService;

    @DisplayName("GET /api/points")
    @Nested
    class GetPoints {

        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다")
        @Test
        void getUserPoint_WithValidId_ReturnsPoint() {
            // given
            String userId = "testUser";
            int point = 1500;

            when(pointService.findUserPoint(userId)).thenReturn(point);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Id", userId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<Integer> response = testRestTemplate.exchange(
                    ENDPOINT_GET_POINTS,
                    HttpMethod.GET,
                    httpEntity,
                    Integer.class
            );

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isEqualTo(1500)
            );
        }

        @Test
        @DisplayName("User-Id 헤더가 없을 경우, 400 Bad Request 응답을 반환한다")
        void getUserPoint_WithoutHeader_ReturnsBadRequest() {
            // when
            HttpEntity<Void> httpEntity = new HttpEntity<>(new HttpHeaders());
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_POINTS,
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("POST /api/points/charge")
    @Nested
    class ChargePoints {

        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다")
        @Test
        void chargePoint_WithExistingUserAnd1000Won_ReturnsUpdatedPoints() {
            // given
            String userId = "existingUser";
            int chargeAmount = 1000;
            int newTotalPoints = 1500;

            when(pointService.chargePoint(userId, chargeAmount)).thenReturn(newTotalPoints);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Id", userId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Integer> httpEntity = new HttpEntity<>(chargeAmount, headers);

            // when
            ResponseEntity<Integer> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINTS,
                    HttpMethod.POST,
                    httpEntity,
                    Integer.class
            );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody()).isEqualTo(1500)
            );
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void chargePoint_WithNonExistentUser_ReturnsNotFound() {
            // given
            String nonExistentUserId = "nonExistentUser";
            int chargeAmount = 1000;

            when(pointService.chargePoint(nonExistentUserId, chargeAmount))
                    .thenThrow(new CoreException(ErrorType.USER_NOT_FOUND));

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Id", nonExistentUserId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Integer> httpEntity = new HttpEntity<>(chargeAmount, headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINTS,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("POST /api/points/use")
    @Nested
    class UsePoints {

        @DisplayName("충분한 포인트가 있을 때 사용에 성공할 경우, 남은 포인트를 응답으로 반환한다")
        @Test
        void usePoint_WithSufficientPoints_ReturnsRemainingPoints() {
            // given
            String userId = "testUser";
            int useAmount = 500;
            int remainingPoints = 1000;

            when(pointService.usePoint(userId, useAmount)).thenReturn(remainingPoints);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Id", userId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Integer> httpEntity = new HttpEntity<>(useAmount, headers);

            // when
            ResponseEntity<Integer> response = testRestTemplate.exchange(
                    ENDPOINT_USE_POINTS,
                    HttpMethod.POST,
                    httpEntity,
                    Integer.class
            );

            // then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isEqualTo(remainingPoints)
            );
        }

        @DisplayName("포인트가 부족할 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void usePoint_WithInsufficientPoints_ReturnsBadRequest() {
            // given
            String userId = "testUser";
            int useAmount = 2000;

            when(pointService.usePoint(userId, useAmount))
                    .thenThrow(new CoreException(ErrorType.INSUFFICIENT_POINTS));

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Id", userId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Integer> httpEntity = new HttpEntity<>(useAmount, headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_USE_POINTS,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}