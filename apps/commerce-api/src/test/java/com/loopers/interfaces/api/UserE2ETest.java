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

    private static final String ENDPOINT_REGISTER = "/api/users/register";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @DisplayName("POST /api/users/register")
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
}
