package com.company.payments.controller;

import com.company.payments.domain.User;
import com.company.payments.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String phone = body.get("phone");
            String password = body.get("password");
            String deviceFp = body.getOrDefault("device_fingerprint", "unknown");

            User user = userService.signup(email, phone, password, deviceFp);
            return ResponseEntity.ok(Map.of(
                    "user_id", user.getId().toString(),
                    "email", user.getEmail(),
                    "kyc_level", user.getKycLevel()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<User> userOpt = userService.login(email, password);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        // In production: return JWT token signed with HSM-managed key
        return ResponseEntity.ok(Map.of(
                "user_id", user.getId().toString(),
                "email", user.getEmail(),
                "kyc_level", user.getKycLevel(),
                "token", "jwt_placeholder_" + user.getId()
        ));
    }
}
