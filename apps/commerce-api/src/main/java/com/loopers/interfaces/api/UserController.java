package com.loopers.interfaces.api;

import com.loopers.domain.example.UserService;
import com.loopers.domain.example.User;
import com.loopers.support.error.CoreException;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        User user = userService.findUser(userId);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }

}
