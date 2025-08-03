package com.loopers.interfaces.api;

import com.loopers.domain.user.User;
import com.loopers.application.user.UserFacade;
import com.loopers.application.point.PointFacade;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.BeforeEach;
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

    private static final String ENDPOINT_REGISTER = "/api/v1/users";
    private static final String ENDPOINT_GET_USER = "/api/v1/users/me";
    private static final String ENDPOINT_GET_POINTS = "/api/v1/points";
    
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @MockitoBean
    private UserFacade userFacade;

    @MockitoBean
    private PointFacade pointFacade;

    @BeforeEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/users/register")
    @Nested
    class Register {

        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다")
        @Test
        void registerUser_WithValidData_ReturnsUserInfo() {
            // arrange
            User request = new User("testUser", "MALE", java.time.LocalDate.of(1990, 1, 1), "test@email.com");
            User savedUser = new User("testUser", "MALE", java.time.LocalDate.of(1990, 1, 1), "test@email.com");

            when(userFacade.registerUser("testUser", "MALE", java.time.LocalDate.of(1990, 1, 1), "test@email.com"))
                    .thenReturn(savedUser);

            HttpEntity<User> httpEntity = new HttpEntity<>(request);

            // act
            ResponseEntity<User> response =
                    testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, httpEntity, User.class);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().getUserId()).isEqualTo("testUser"),
                    () -> assertThat(response.getBody().getEmail()).isEqualTo("test@email.com"),
                    () -> assertThat(response.getBody().getBirthDate()).isEqualTo(java.time.LocalDate.of(1990, 1, 1))
            );
        }


        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void registerUser_WithMissingGender_ReturnsBadRequest() {
            // arrange
            // gender가 없는 상황을 Mock으로 시뮬레이션
            when(userFacade.registerUser(any(String.class), any(String.class), any(java.time.LocalDate.class), any(String.class)))
                    .thenThrow(new CoreException(ErrorType.INVALID_GENDER, "Gender is required"));

            User request = new User("testUser", "MALE", java.time.LocalDate.of(1990, 1, 1), "test@email.com");
            HttpEntity<User> httpEntity = new HttpEntity<>(request);

            // act
            ResponseEntity<String> response =
                    testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, httpEntity, String.class);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/users/me")
    @Nested
    class GetUser {

        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다")
        @Test
        void getUser_WithExistingId_ReturnsUserInfo() {
            // given
            String userId = "testUser";
            User existingUser = new User(userId, "MALE", java.time.LocalDate.of(1990, 1, 1), "test@email.com");

            when(userFacade.getUserInfo(userId)).thenReturn(java.util.Optional.of(existingUser));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<User> response = testRestTemplate.exchange(
                    ENDPOINT_GET_USER,
                    HttpMethod.GET,
                    httpEntity,
                    User.class
            );

            // then
            assertAll(
                    () -> {
                        System.out.println("Status Code: " + response.getStatusCode());
                        System.out.println("Response Body: " + response.getBody());
                        assertTrue(response.getStatusCode().is2xxSuccessful());
                    },
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().getUserId()).isEqualTo(userId),
                    () -> assertThat(response.getBody().getEmail()).isEqualTo("test@email.com"),
                    () -> assertThat(response.getBody().getBirthDate()).isEqualTo(java.time.LocalDate.of(1990, 1, 1)),
                    () -> assertThat(response.getBody().getGender()).isEqualTo("MALE")
            );
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void getUser_WithNonExistingId_ReturnsNotFound() {
            // given
            String nonExistingUserId = "nonExistingUser";

            when(userFacade.getUserInfo(nonExistingUserId))
                    .thenReturn(java.util.Optional.empty());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", nonExistingUserId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_GET_USER,
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다")
    @Test
    void getUserPoint_WithValidId_ReturnsPoint() {
        // given
        String userId = "testUser";
        java.math.BigDecimal point = java.math.BigDecimal.valueOf(1500);

        when(pointFacade.getPoints(userId)).thenReturn(point);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", userId);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        // when
        ResponseEntity<java.math.BigDecimal> response = testRestTemplate.exchange(
                ENDPOINT_GET_POINTS,
                HttpMethod.GET,
                httpEntity,
                java.math.BigDecimal.class
        );

        // then
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isEqualTo(java.math.BigDecimal.valueOf(1500))
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // 이제 400 반환됨
    }

    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다")
    @Test
    void chargePoint_WithExistingUserAnd1000Won_ReturnsUpdatedPoints() {
        // given
        String userId = "existingUser";
        java.math.BigDecimal chargeAmount = java.math.BigDecimal.valueOf(1000);
        java.math.BigDecimal newTotalPoints = java.math.BigDecimal.valueOf(1500); // 충전 후 총 포인트

        // Mock 설정: 충전 성공 시 새로운 총 포인트 반환
        when(pointFacade.chargePoints(userId, chargeAmount)).thenReturn(newTotalPoints);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // RequestBody가 BigDecimal이므로 BigDecimal로 전달
        HttpEntity<java.math.BigDecimal> httpEntity = new HttpEntity<>(chargeAmount, headers);

        // when
        ResponseEntity<java.math.BigDecimal> response = testRestTemplate.exchange(
                "/api/v1/points/charge",
                HttpMethod.POST,
                httpEntity,
                java.math.BigDecimal.class
        );

        // then
        assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).isEqualTo(java.math.BigDecimal.valueOf(1500)) // 충전된 보유 총량
        );

    }

    @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다")
    @Test
    void chargePoint_WithNonExistentUser_ReturnsNotFound() {
        // given
        String nonExistentUserId = "nonExistentUser";
        java.math.BigDecimal chargeAmount = java.math.BigDecimal.valueOf(1000);

        // Mock 설정: 존재하지 않는 사용자에 대해 USER_NOT_FOUND 예외 발생
        when(pointFacade.chargePoints(nonExistentUserId, chargeAmount))
                .thenThrow(new CoreException(ErrorType.USER_NOT_FOUND));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", nonExistentUserId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // RequestBody가 BigDecimal이므로 BigDecimal로 전달
        HttpEntity<java.math.BigDecimal> httpEntity = new HttpEntity<>(chargeAmount, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/v1/points/charge",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        // then
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
        );

    }
}
