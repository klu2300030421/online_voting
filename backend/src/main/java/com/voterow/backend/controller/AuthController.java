package com.voterow.backend.controller;

import com.voterow.backend.dto.LoginRequest;
import com.voterow.backend.dto.SignUpRequest;
import com.voterow.backend.dto.UpdateProfileRequest;
import com.voterow.backend.model.User;
import com.voterow.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"})
public class AuthController {
    
    // These must be final
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            User user = authService.registerUser(signUpRequest);
            return ResponseEntity.ok("User registered successfully: " + user.getEmail());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user details and return them
            User user = authService.getUserByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.status(404).body("User not found.");
            }
            
            // Return complete user information as JSON
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("age", user.getAge());
            userInfo.put("phoneNumber", user.getPhoneNumber());
            userInfo.put("idProofNumber", user.getIdProofNumber());
            userInfo.put("address", user.getAddress());
            userInfo.put("userType", user.getUserType().name());
            userInfo.put("adminRole", user.getAdminRole());
            userInfo.put("isActive", user.getIsActive());
            userInfo.put("isVerified", user.getIsVerified());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("No user is currently authenticated.");
        }
        
        // Get user details from database
        User user = authService.getUserByEmail(principal.getName());
        if (user == null) {
            return ResponseEntity.status(404).body("User not found.");
        }
        
        // Return complete user information as JSON
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("age", user.getAge() != null ? user.getAge() : 0);
        userInfo.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        userInfo.put("idProofNumber", user.getIdProofNumber() != null ? user.getIdProofNumber() : "");
        userInfo.put("address", user.getAddress() != null ? user.getAddress() : "");
        userInfo.put("userType", user.getUserType().name());
        userInfo.put("adminRole", user.getAdminRole() != null ? user.getAdminRole().name() : "");
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("isVerified", user.getIsVerified());
        
        return ResponseEntity.ok(userInfo);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest, @RequestParam String email) {
        try {
            User updatedUser = authService.updateUserProfile(email, updateRequest);
            
            // Return updated user information
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", updatedUser.getId());
            userInfo.put("email", updatedUser.getEmail());
            userInfo.put("fullName", updatedUser.getFullName());
            userInfo.put("age", updatedUser.getAge() != null ? updatedUser.getAge() : 0);
            userInfo.put("phoneNumber", updatedUser.getPhoneNumber() != null ? updatedUser.getPhoneNumber() : "");
            userInfo.put("idProofNumber", updatedUser.getIdProofNumber() != null ? updatedUser.getIdProofNumber() : "");
            userInfo.put("address", updatedUser.getAddress() != null ? updatedUser.getAddress() : "");
            userInfo.put("userType", updatedUser.getUserType().name());
            userInfo.put("adminRole", updatedUser.getAdminRole() != null ? updatedUser.getAdminRole().name() : "");
            userInfo.put("isActive", updatedUser.getIsActive());
            userInfo.put("isVerified", updatedUser.getIsVerified());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update profile: " + e.getMessage());
        }
    }
}