package com.campusrecycle.controller;

import com.campusrecycle.dto.UserDto;
import com.campusrecycle.model.User;
import com.campusrecycle.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(UserDto.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserDto>> getLeaderboard() {
        List<UserDto> leaderboard = userService.findAllUsers().stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .limit(20)
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(leaderboard);
    }

    @PostMapping("/me/points")
    public ResponseEntity<UserDto> addPoints(Authentication authentication,
                                              @RequestBody Map<String, Integer> body) {
        Long userId = Long.parseLong(authentication.getName());
        int points = body.getOrDefault("points", 0);
        User user = userService.addPoints(userId, points);
        return ResponseEntity.ok(UserDto.from(user));
    }
}
