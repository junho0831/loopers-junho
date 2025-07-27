package com.loopers.domain.example;

import com.loopers.support.error.CoreException;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.*;

public class UserUnitTest {

    @Test
    @DisplayName("ID가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다")
    public void createUser_WithInvalidId_ThrowsException() {
        assertThrows(CoreException.class, () -> {
            new User("tooLongUserId123", "test@email.com", "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User("user@123", "test@email.com", "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User("", "test@email.com", "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User(null, "test@email.com", "1990-01-01", Gender.MALE);
        });
    }

    @Test
    @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다")
    public void createUser_WithInvalidEmail_ThrowsException() {
        assertThrows(CoreException.class, () -> {
            new User("validId", null, "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User("validId", "invalid-email", "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User("validId", "invalid@email", "1990-01-01", Gender.MALE);
        });

        assertThrows(CoreException.class, () -> {
            new User("validId", "@email.com", "1990-01-01", Gender.MALE);
        });
    }
    // TODO: Point 엔티티로 이동 예정
    // @Test
    // @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다")
    // public void chargePoint_WithInvalidAmount_ThrowsException() {
    //     // given
    //     User user = new User("validId", "test@email.com", "1990-01-01", Gender.MALE, 100);
    //     // when & then
    //     assertThrows(CoreException.class, () -> {
    //         user.setPoint(-100);
    //     });

    //     assertEquals(100, user.getPoint());
    // }
}
