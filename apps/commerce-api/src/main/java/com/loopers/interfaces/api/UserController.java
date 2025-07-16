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
        try {
            User user = userService.registerUser(
                    request.getUserId(),
                    request.getEmail(),
                    request.getBirthday().toString(),
                    request.getGender()
            );

            return ResponseEntity.ok(user);
        } catch (CoreException e) {
            return ResponseEntity.status(e.getErrorType().getStatus()).build();
        }
    }
    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader("User-Id") String userId) {
        try {
            User user = userService.findUser(userId);
            return ResponseEntity.ok(user);
        } catch (CoreException e) {
            return ResponseEntity.status(e.getErrorType().getStatus()).build();
        }
    }


    @GetMapping("/points")
    public ResponseEntity<Integer> points(@RequestHeader("User-Id") String userId) {
        try {
            int userPoint = userService.findUserPoint(userId);
            return ResponseEntity.ok(userPoint);
        } catch (CoreException e) {
            return ResponseEntity.status(e.getErrorType().getStatus()).build();
        }
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Void> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest().build(); // 400 반환
    }
}


