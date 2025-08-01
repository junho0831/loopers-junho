package com.loopers.interfaces.api;

import com.loopers.application.point.PointFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointFacade pointFacade;

    public PointController(PointFacade pointFacade) {
        this.pointFacade = pointFacade;
    }

    @GetMapping
    public ResponseEntity<BigDecimal> getPoints(@RequestHeader("X-USER-ID") String userId) {
        BigDecimal userPoint = pointFacade.getPoints(userId);
        return ResponseEntity.ok(userPoint);
    }

    @PostMapping("/charge")
    public ResponseEntity<BigDecimal> chargePoints(@RequestHeader("X-USER-ID") String userId,
                                               @RequestBody BigDecimal amount) {
        BigDecimal newTotalPoints = pointFacade.chargePoints(userId, amount);
        return ResponseEntity.ok(newTotalPoints);
    }

    @PostMapping("/use")
    public ResponseEntity<BigDecimal> usePoints(@RequestHeader("X-USER-ID") String userId,
                                            @RequestBody BigDecimal amount) {
        BigDecimal remainingPoints = pointFacade.usePoints(userId, amount);
        return ResponseEntity.ok(remainingPoints);
    }
}