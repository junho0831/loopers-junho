package com.loopers.domain.example;

import com.loopers.infrastructure.example.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String userId, String email, String birthday, Gender gender) {
        if (userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.USER_ALREADY_EXISTS);
        }

        User user = new User(userId, email, birthday, gender);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findUser(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

}
