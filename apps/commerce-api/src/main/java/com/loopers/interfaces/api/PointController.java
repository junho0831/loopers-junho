package com.loopers.interfaces.api;

import com.loopers.domain.example.PointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping
    public ResponseEntity<Integer> getPoints(@RequestHeader("X-USER-ID") String userId) {
        int userPoint = pointService.findUserPoint(userId);
        return ResponseEntity.ok(userPoint);
    }

    @PostMapping("/charge")
    public ResponseEntity<Integer> chargePoints(@RequestHeader("X-USER-ID") String userId,
                                               @RequestBody int amount) {
        int newTotalPoints = pointService.chargePoint(userId, amount);
        return ResponseEntity.ok(newTotalPoints);
    }

    @PostMapping("/use")
    public ResponseEntity<Integer> usePoints(@RequestHeader("X-USER-ID") String userId,
                                            @RequestBody int amount) {
        int remainingPoints = pointService.usePoint(userId, amount);
        return ResponseEntity.ok(remainingPoints);
    }
}