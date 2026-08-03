package com.voterow.backend.controller;

import com.voterow.backend.model.User;
import com.voterow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple debug controller to inspect stored user rows while developing.
 * This endpoint is intentionally behind a runtime flag `app.debug` and
 * available under /api/auth/debug so it inherits the existing permitAll
 * rule for /api/auth/**. Do NOT enable app.debug in production.
 */
@RestController
@RequestMapping("/api/auth/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.debug:false}")
    private boolean appDebug;

    @GetMapping("/user")
    public ResponseEntity<?> getUserForDebug(@RequestParam String email) {
        if (!appDebug) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Debug endpoint disabled");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("id", user.getId());
        out.put("email", user.getEmail());
        out.put("passwordHash", user.getPassword());
        out.put("isActive", user.getIsActive());
        out.put("isVerified", user.getIsVerified());
        out.put("userType", user.getUserType() != null ? user.getUserType().name() : null);
        out.put("lastLoginAt", user.getLastLoginAt());

        return ResponseEntity.ok(out);
    }

    /**
     * Reset password and activate the account for the given email. This endpoint is
     * a development helper and must be disabled in production by setting app.debug=false.
     */
    @GetMapping("/reset")
    public ResponseEntity<?> resetPasswordForDebug(@RequestParam String email, @RequestParam String password) {
        if (!appDebug) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Debug endpoint disabled");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsVerified(true);
        userRepository.save(user);

        Map<String, Object> out = new HashMap<>();
        out.put("message", "Password reset and account activated for " + email);
        out.put("email", user.getEmail());
        return ResponseEntity.ok(out);
    }
}
