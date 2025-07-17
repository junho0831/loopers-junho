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
        return userRepository.findByUserId(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public int findUserPoint(String userId) {
        return userRepository.findUserPointByUserId(userId);
    }

    public int chargePoint(String userId, int chargeAmount) {
        if (chargeAmount < 0) {
            throw new CoreException(ErrorType.INVALID_CHARGE_AMOUNT);
        }
        int currentPoints = userRepository.findUserPointByUserId(userId);

        int newPoints = currentPoints + chargeAmount;

        int updatedRows = userRepository.updateUserPoints(userId, newPoints);

        if (updatedRows == 0) {
            throw new CoreException(ErrorType.USER_NOT_FOUND);
        }

        return updatedRows;
    }
}
