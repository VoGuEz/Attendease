package com.attendease.controller;

import com.attendease.entity.User;
import com.attendease.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        User user = userService.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
            "id",       user.getId(),
            "email",    user.getEmail(),
            "fullName", user.getFullName(),
            "role",     user.getRole().name()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
        @RequestBody Map<String, String> body,
        Authentication auth
    ) {
        User user = userService.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("fullName")) user.setFullName(body.get("fullName"));
        userService.save(user);

        return ResponseEntity.ok(Map.of(
            "id",       user.getId(),
            "email",    user.getEmail(),
            "fullName", user.getFullName(),
            "role",     user.getRole().name()
        ));
    }
}
