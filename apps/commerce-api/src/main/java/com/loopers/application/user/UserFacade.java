package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Point;
import com.loopers.infrastructure.user.JpaUserRepository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserFacade {
    private final JpaUserRepository userRepository;

    public UserFacade(JpaUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String userId, String gender, LocalDate birthDate, String email) {
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("User with this ID already exists.");
        }
        User newUser = new User(userId, Gender.valueOf(gender), birthDate, new Email(email), new Point(0));
        return userRepository.save(newUser);
    }

    public Optional<User> getUserInfo(String userId) {
        return userRepository.findByUserId(userId);
    }
}