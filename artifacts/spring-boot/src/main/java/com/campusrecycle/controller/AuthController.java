package com.campusrecycle.controller;

import com.campusrecycle.dto.UserDto;
import com.campusrecycle.model.User;
import com.campusrecycle.security.JwtTokenProvider;
import com.campusrecycle.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public AuthController(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(authentication.getName());
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(UserDto.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        return ResponseEntity.ok(Map.of("valid", true, "userId", authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/login/github")
    public ResponseEntity<Map<String, String>> githubLoginUrl() {
        return ResponseEntity.ok(Map.of(
            "url", "/api/oauth2/authorization/github",
            "provider", "github"
        ));
    }
}
