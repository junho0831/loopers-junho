package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Point;
import com.loopers.domain.user.UserService;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserFacade {
    private final UserService userService;

    public UserFacade(UserService userService) {
        this.userService = userService;
    }

    public User registerUser(String userId, String gender, LocalDate birthDate, String email) {
        if (userService.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("User with this ID already exists.");
        }
        User newUser = new User(userId, Gender.valueOf(gender), birthDate, new Email(email), new Point(0));
        return userService.saveUser(newUser);
    }

    public Optional<User> getUserInfo(String userId) {
        return userService.findByUserId(userId);
    }
}