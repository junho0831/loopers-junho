package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PointFacade {
    private final PointService pointService;

    public PointFacade(PointService pointService) {
        this.pointService = pointService;
    }

    public BigDecimal chargePoints(String userId, BigDecimal amount) {
        Point point = pointService.findByUserId(userId);
        point.addPoints(amount);
        Point savedPoint = pointService.savePoint(point);
        return savedPoint.getPointBalance();
    }

    public BigDecimal getPoints(String userId) {
        Point point = pointService.findByUserId(userId);
        return point.getPointBalance();
    }

    public BigDecimal usePoints(String userId, BigDecimal amount) {
        Point point = pointService.findByUserId(userId);
        if (point.getPointBalance().equals(BigDecimal.ZERO)) {
            throw new IllegalArgumentException("User not found");
        }
        point.deductPoints(amount);
        Point savedPoint = pointService.savePoint(point);
        return savedPoint.getPointBalance();
    }
}