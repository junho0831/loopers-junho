package com.loopers.domain.example;

import com.loopers.infrastructure.example.UserRepository;
import com.loopers.support.error.CoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원 가입시 User 저장이 수행된다 (spy 검증)")
    public void registerUser_CallsSaveMethod() {
        // given
        String userId = "testUser";
        String email = "test@email.com";
        String birthday = "1990-01-01";

        User savedUser = new User(userId, email, birthday, Gender.MALE);

        when(userRepository.existsByUserId(userId)).thenReturn(false); //중복 아님
        when(userRepository.save(any(User.class))).thenReturn(savedUser); // 무조건 세이브

        // when
        User result = userService.registerUser(userId, email, birthday, Gender.MALE);

        // then
        verify(userRepository, times(1)).existsByUserId(userId);
        verify(userRepository, times(1)).save(any(User.class));
        assertEquals(userId, result.getUserId());
        assertEquals(email, result.getEmail());
    }

    @Test
    @DisplayName("이미 가입된 ID로 회원가입 시도 시, 실패한다")
    public void registerUser_WithExistingId_ThrowsException() {
        // given
        String existingUserId = "existingUser";
        when(userRepository.existsByUserId(existingUserId)).thenReturn(true);

        // when
        CoreException exception = assertThrows(CoreException.class, () -> {
            userService.registerUser(existingUserId, "test@email.com", "1990-01-01", Gender.MALE);
        });

        // then
        assertEquals("이미 가입된 ID입니다", exception.getMessage());
        verify(userRepository, times(1)).existsByUserId(existingUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다")
    public void findUser_WithExistingId_ReturnsUser() {
        // given
        String userId = "testUser";
        User existingUser = new User(userId, "test@email.com", "1990-01-01", Gender.MALE);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(existingUser));

        // when
        User result = userService.findUser(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("test@email.com", result.getEmail());
        assertEquals("1990-01-01", result.getBirthday().toString());
        assertEquals(Gender.MALE, result.getGender());
        verify(userRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("해당 ID의 회원이 존재하지 않을 경우, 예외가 발생한다")
    public void findUser_WithNonExistingId_ThrowsException() {
        // given
        String userid = "userid";
        when(userRepository.findByUserId(userid)).thenReturn(Optional.empty());

        // when & then
        CoreException exception = assertThrows(CoreException.class, () -> {
            userService.findUser(userid);
        });

        assertEquals("사용자를 찾을 수 없습니다", exception.getMessage());
        verify(userRepository, times(1)).findByUserId(userid);
    }
}
