package com.loopers.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("정상적인 정보로 사용자를 생성할 수 있다")
    void createUser() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when
        User user = new User(userId, gender, birthDate, email);

        // then
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getGender()).isEqualTo(gender);
        assertThat(user.getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("사용자 ID가 null이면 예외가 발생한다")
    void createUserWithNullUserId() {
        // given
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(null, gender, birthDate, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null or empty");
    }

    @Test
    @DisplayName("사용자 ID가 빈 문자열이면 예외가 발생한다")
    void createUserWithEmptyUserId() {
        // given
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User("", gender, birthDate, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null or empty");
    }

    @Test
    @DisplayName("이메일이 null이면 예외가 발생한다")
    void createUserWithNullEmail() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);

        // when & then
        assertThatThrownBy(() -> new User(userId, gender, birthDate, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");
    }

    @Test
    @DisplayName("이메일이 빈 문자열이면 예외가 발생한다")
    void createUserWithEmptyEmail() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);

        // when & then
        assertThatThrownBy(() -> new User(userId, gender, birthDate, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");
    }

    @Test
    @DisplayName("잘못된 이메일 형식이면 예외가 발생한다")
    void createUserWithInvalidEmail() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String invalidEmail = "invalid-email";

        // when & then
        assertThatThrownBy(() -> new User(userId, gender, birthDate, invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("성별이 null이면 예외가 발생한다")
    void createUserWithNullGender() {
        // given
        String userId = "user1";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(userId, null, birthDate, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gender cannot be null or empty");
    }

    @Test
    @DisplayName("잘못된 성별이면 예외가 발생한다")
    void createUserWithInvalidGender() {
        // given
        String userId = "user1";
        String invalidGender = "INVALID";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(userId, invalidGender, birthDate, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid gender");
    }

    @Test
    @DisplayName("생년월일이 null이면 예외가 발생한다")
    void createUserWithNullBirthDate() {
        // given
        String userId = "user1";
        String gender = "MALE";
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(userId, gender, null, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth date cannot be null");
    }

    @Test
    @DisplayName("미래의 생년월일이면 예외가 발생한다")
    void createUserWithFutureBirthDate() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate futureDate = LocalDate.now().plusDays(1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(userId, gender, futureDate, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth date cannot be in the future");
    }

    @Test
    @DisplayName("동일한 사용자 ID를 가진 사용자는 같다고 판단된다")
    void equalsUser() {
        // given
        User user1 = new User("user1", "MALE", LocalDate.of(1990, 1, 1), "test1@example.com");
        User user2 = new User("user1", "FEMALE", LocalDate.of(1995, 5, 5), "test2@example.com");
        User user3 = new User("user2", "MALE", LocalDate.of(1990, 1, 1), "test1@example.com");

        // when & then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }
}