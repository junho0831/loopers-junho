package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.infrastructure.point.JpaPointRepository;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PointFacade {
    private final JpaPointRepository pointRepository;

    public PointFacade(JpaPointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    public BigDecimal chargePoints(String userId, BigDecimal amount) {
        Point point = pointRepository.findByUserId(userId)
                .orElse(new Point(userId, BigDecimal.ZERO));
        point.addPoints(amount);
        Point savedPoint = pointRepository.save(point);
        return savedPoint.getPointBalance();
    }

    public BigDecimal getPoints(String userId) {
        Point point = pointRepository.findByUserId(userId)
                .orElse(new Point(userId, BigDecimal.ZERO));
        return point.getPointBalance();
    }

    public BigDecimal usePoints(String userId, BigDecimal amount) {
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        point.deductPoints(amount);
        Point savedPoint = pointRepository.save(point);
        return savedPoint.getPointBalance();
    }
}