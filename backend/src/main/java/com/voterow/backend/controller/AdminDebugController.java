package com.voterow.backend.controller;

import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"})

public class AdminDebugController {
    
    private final UserRepository userRepository;
    private final AuthService authService;
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users.stream().map(user -> {
            return String.format("ID: %d, Email: %s, Type: %s, Active: %s", 
                user.getId(), user.getEmail(), user.getUserType(), user.getIsActive());
        }).collect(Collectors.toList()));
    }
    
    @GetMapping("/admins")
    public ResponseEntity<?> getAdminUsers() {
        List<User> admins = userRepository.findByUserType(UserType.ROLE_ADMIN);
        return ResponseEntity.ok(admins.stream().map(user -> {
            return String.format("ID: %d, Email: %s, Type: %s, AdminRole: %s, Active: %s", 
                user.getId(), user.getEmail(), user.getUserType(), 
                user.getAdminRole(), user.getIsActive());
        }).collect(Collectors.toList()));
    }
    
    @GetMapping("/create-test-admin")
    public ResponseEntity<?> createTestAdmin() {
        try {
            // Create test admin with known credentials
            User admin = authService.createAdminUser("admin@test.com", "admin123", "Test Admin");
            return ResponseEntity.ok("Test admin created: " + admin.getEmail() + " (password: admin123)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create admin: " + e.getMessage());
        }
    }
}
