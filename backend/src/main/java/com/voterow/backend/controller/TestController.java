package com.voterow.backend.controller;

import com.voterow.backend.model.AdminRole;
import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestParam String email, @RequestParam String password) {
        try {
            // Check if user exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body("User already exists");
            }
            
            User admin = new User();
            admin.setFullName("Test Admin");
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setAge(30);
            admin.setUserType(UserType.ROLE_ADMIN);
            admin.setAdminRole(AdminRole.SUPER_ADMIN);
            admin.setIsActive(true);
            admin.setIsVerified(true);
            admin.setTwoFactorEnabled(false);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(admin);
            
            return ResponseEntity.ok("Admin created: " + email + " with password: " + password);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestParam String email, @RequestParam String password) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            return ResponseEntity.ok("Password matches: " + matches + 
                " | Stored hash: " + user.getPassword().substring(0, 20) + "..." +
                " | User active: " + user.getIsActive());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}