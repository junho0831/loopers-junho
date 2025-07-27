package com.loopers.interfaces.api;

import com.loopers.domain.example.UserService;
import com.loopers.domain.example.User;
import com.loopers.support.error.CoreException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User request) {
        User user = userService.registerUser(
                request.getUserId(),
                request.getEmail(),
                request.getBirthday().toString(),
                request.getGender()
        );

        return ResponseEntity.ok(user);
    }
    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader("User-Id") String userId) {
        User user = userService.findUser(userId);
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


