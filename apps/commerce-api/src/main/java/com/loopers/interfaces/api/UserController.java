package com.loopers.interfaces.api;

import com.loopers.application.user.UserFacade;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @PostMapping
    public ResponseEntity<User> registerUser(@RequestBody User request) {
        User user = userFacade.registerUser(
                request.getUserId(),
                request.getGender().name(),
                request.getBirthDate(),
                request.getEmail().getValue()
        );

        return ResponseEntity.ok(user);
    }
    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader("X-USER-ID") String userId) {
        User user = userFacade.getUserInfo(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(user);
    }


    // TODO: Point 엔티티로 이동 예정
    // @GetMapping("/points")
    // public ResponseEntity<Integer> points(@RequestHeader("User-Id") String userId) {
    //     int userPoint = userService.findUserPoint(userId);
    //     return ResponseEntity.ok(userPoint);
    // }

    // @PostMapping("/charge")
    // public ResponseEntity<Integer> chargePoints(@RequestHeader("User-Id") String userId,
    //                                             @RequestBody int amount) {
    //     int newTotalPoints = userService.chargePoint(userId, amount);
    //     return ResponseEntity.ok(newTotalPoints);
    // }

}


