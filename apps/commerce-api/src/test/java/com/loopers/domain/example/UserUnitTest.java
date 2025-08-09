package com.loopers.domain.example;

import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.*;

public class UserUnitTest {

    @Test
    @DisplayName("ID가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다")
    public void createUser_WithInvalidId_ThrowsException() {
        assertThrows(CoreException.class, () -> {
            new User("tooLongUserId123", new Email("test@email.com"), "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User("", new Email("test@email.com"), "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User(null, new Email("test@email.com"), "1990-01-01", Gender.MALE);
        });
    }

    @Test
    @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다")
    public void createUser_WithInvalidEmail_ThrowsException() {
        assertThrows(CoreException.class, () -> {
            new User("validId", new Email(null), "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new Email("invalid-email");
        });

        assertThrows(CoreException.class, () -> {
            new Email("invalid@email");
        });

        assertThrows(CoreException.class, () -> {
            new Email("@email.com");
        });
    }
}
