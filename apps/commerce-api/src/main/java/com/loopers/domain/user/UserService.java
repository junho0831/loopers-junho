package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String userId, Gender gender, LocalDate localDate, Email email, Point point)  {
        if (userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.USER_ALREADY_EXISTS);
        }

        User user = new User(userId, gender, localDate, email, point);
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
         User user = userRepository.findByUserId(userId)
                 .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

         user.getPoint().charge(chargeAmount);

         return user.getPoint().getAmount();
     }
}
