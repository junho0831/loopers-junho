package com.loopers.domain.example;

import com.loopers.infrastructure.example.PointRepository;
import com.loopers.infrastructure.example.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    public PointService(PointRepository pointRepository, UserRepository userRepository) {
        this.pointRepository = pointRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public int findUserPoint(String userId) {
        validateUserExists(userId);
        return pointRepository.findByUserId(userId)
                .map(Point::getAmount)
                .orElse(0);
    }

    public int chargePoint(String userId, int chargeAmount) {
        validateUserExists(userId);
        
        Point point = pointRepository.findByUserId(userId)
                .orElse(new Point(userId, 0));
        
        point.charge(chargeAmount);
        Point savedPoint = pointRepository.save(point);
        
        return savedPoint.getAmount();
    }

    public int usePoint(String userId, int useAmount) {
        validateUserExists(userId);
        
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.INSUFFICIENT_POINTS));
        
        point.use(useAmount);
        Point savedPoint = pointRepository.save(point);
        
        return savedPoint.getAmount();
    }

    private void validateUserExists(String userId) {
        if (!userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.USER_NOT_FOUND);
        }
    }
}