package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
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
        User user = new User(userId, Gender.MALE, birthDate, new Email(email), new Point(0));

        // then
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getGender()).isEqualTo(Gender.MALE);
        assertThat(user.getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getEmail().getValue()).isEqualTo(email);
    }

    @Test
    @DisplayName("사용자 ID가 null이면 예외가 발생한다")
    void createUserWithNullUserId() {
        // given
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(null, Gender.MALE, birthDate, new Email(email), new Point(0)))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("사용자 ID가 빈 문자열이면 예외가 발생한다")
    void createUserWithEmptyUserId() {
        // given
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User("", Gender.MALE, birthDate, new Email(email), new Point(0)))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("이메일이 null이면 예외가 발생한다")
    void createUserWithNullEmail() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);

        // when & then
        assertThatThrownBy(() -> new User(userId, Gender.MALE, birthDate, null, new Point(0)))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("이메일이 빈 문자열이면 예외가 발생한다")
    void createUserWithEmptyEmail() {
        // given
        String userId = "user1";
        String gender = "MALE";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);

        // when & then
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(CoreException.class);
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
        assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("성별이 null이면 예외가 발생한다")
    void createUserWithNullGender() {
        // given
        String userId = "user1";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(userId, null, birthDate, new Email(email), new Point(0)))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("잘못된 성별이면 예외가 발생한다")
    void createUserWithInvalidGender() {
        // given
        String userId = "user1";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String email = "test@example.com";
        String invalidGender = "INVALID";

        // when & then
        // Test converting invalid string to Gender enum
        assertThatThrownBy(() -> Gender.valueOf(invalidGender))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("생년월일이 null이면 예외가 발생한다")
    void createUserWithNullBirthDate() {
        // given
        String userId = "user1";
        String gender = "MALE";
        String email = "test@example.com";

        // when & then
        assertThatThrownBy(() -> new User(userId, Gender.MALE, null, new Email(email), new Point(0)))
                .isInstanceOf(CoreException.class);
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
        assertThatThrownBy(() -> new User(userId, Gender.MALE, futureDate, new Email(email), new Point(0)))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("동일한 사용자 ID를 가진 사용자는 같다고 판단된다")
    void equalsUser() {
        // given
        User user1 = new User("user1", Gender.MALE, LocalDate.of(1990, 1, 1), new Email("test1@example.com"), new Point(0));
        User user2 = new User("user1", Gender.FEMALE, LocalDate.of(1995, 5, 5), new Email("test2@example.com"), new Point(0));
        User user3 = new User("user2", Gender.MALE, LocalDate.of(1990, 1, 1), new Email("test1@example.com"), new Point(0));

        // when & then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }
}