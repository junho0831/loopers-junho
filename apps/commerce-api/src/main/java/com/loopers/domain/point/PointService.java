package com.loopers.domain.point;

import com.loopers.domain.product.Money;
import com.loopers.infrastructure.point.JpaPointRepository;
import org.springframework.stereotype.Service;

@Service
public class PointService {
    private final JpaPointRepository pointRepository;

    public PointService(JpaPointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    public Point loadUserPointsWithLock(String userId) {
        return pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("User has no points account."));
    }

    public void deductPoints(Point userPoints, Money amount) {
        userPoints.use(amount);
        pointRepository.save(userPoints);
    }

    public void validateUserPoints(Point userPoints, Money requiredAmount) {
        if (!userPoints.hasEnoughPoints(requiredAmount)) {
            throw new IllegalArgumentException("Insufficient points balance.");
        }
    }
    
    public Point findByUserId(String userId) {
        return pointRepository.findByUserId(userId)
                .orElse(new Point(userId, java.math.BigDecimal.ZERO));
    }
    
    public Point savePoint(Point point) {
        return pointRepository.save(point);
    }
}
