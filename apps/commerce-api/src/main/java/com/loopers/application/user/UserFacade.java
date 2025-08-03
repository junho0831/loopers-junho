package com.loopers.application.user;

import com.loopers.domain.user.User;
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
        User newUser = new User(userId, gender, birthDate, email);
        return userRepository.save(newUser);
    }

    public Optional<User> getUserInfo(String userId) {
        return userRepository.findByUserId(userId);
    }
}