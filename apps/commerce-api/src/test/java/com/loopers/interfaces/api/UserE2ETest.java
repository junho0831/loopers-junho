package com.loopers.interfaces.api;

import com.loopers.domain.example.Gender;
import com.loopers.domain.example.User;
import com.loopers.infrastructure.example.UserRepository;
import com.loopers.domain.example.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
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
class UserE2ETest {

    private static final String ENDPOINT_REGISTER = "/api/v1/users";
    private static final String ENDPOINT_GET_USER = "/api/v1/users/me";
    // TODO: Point 엔티티로 이동 예정
    // private static final String ENDPOINT_GET_POINTS = "/api/users/points";
    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @DisplayName("POST /api/v1/users")
    @Nested
    class Register {

        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다")
        @Test
        void registerUser_WithValidData_ReturnsUserInfo() {
            // arrange
            User request = new User("testUser", "test@email.com", "1990-01-01", Gender.MALE);
            User savedUser = new User("testUser", "test@email.com", "1990-01-01", Gender.MALE);

            when(userService.registerUser("testUser", "test@email.com", "1990-01-01", Gender.MALE))
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
                    () -> assertThat(response.getBody().getBirthday()).isEqualTo("1990-01-01")
            );
        }


        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다")
        @Test
        void registerUser_WithMissingGender_ReturnsBadRequest() {
            // arrange
            // gender가 없는 상황을 Mock으로 시뮬레이션
            when(userService.registerUser(any(String.class), any(String.class), any(String.class), any(Gender.class)))
                    .thenThrow(new CoreException(ErrorType.INVALID_GENDER, "Gender is required"));

            User request = new User("testUser", "test@email.com", "1990-01-01", Gender.MALE);
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
            User existingUser = new User(userId, "test@email.com", "1990-01-01", Gender.MALE);

            when(userService.findUser(userId)).thenReturn(existingUser);

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
                    () -> assertThat(response.getBody().getBirthday().toString()).isEqualTo("1990-01-01"),
                    () -> assertThat(response.getBody().getGender()).isEqualTo(Gender.MALE)
            );
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void getUser_WithNonExistingId_ReturnsNotFound() {
            // given
            String nonExistingUserId = "nonExistingUser";

            when(userService.findUser(nonExistingUserId))
                    .thenThrow(new CoreException(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

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
}
