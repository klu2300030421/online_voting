package com.voterow.backend.controller;

import com.voterow.backend.dto.LoginRequest;
import com.voterow.backend.dto.ChangePasswordRequest;
import com.voterow.backend.dto.SignUpRequest;
import com.voterow.backend.dto.UpdateProfileRequest;
import com.voterow.backend.model.User;
import com.voterow.backend.security.JwtTokenProvider;
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
public class AuthController {
    
    // These must be final
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

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
            User user = authService.getUserByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid email or password.");
            }
            
            if (!user.getIsActive()) {
                return ResponseEntity.status(401).body("Account is deactivated.");
            }
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String jwt = jwtTokenProvider.generateToken(loginRequest.getEmail());
            user = authService.updateLastLogin(loginRequest.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("type", "Bearer");
            
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
            
            response.put("user", userInfo);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.status(401).body("User not found or account deactivated.");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Authentication failed: " + e.getClass().getSimpleName());
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
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("No user is currently authenticated.");
            }
            User updatedUser = authService.updateUserProfile(principal.getName(), updateRequest);
            
            // Return updated user information
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", updatedUser.getId());
            userInfo.put("email", updatedUser.getEmail());
            userInfo.put("fullName", updatedUser.getFullName());
            userInfo.put("age", updatedUser.getAge() != null ? updatedUser.getAge() : 0);
            userInfo.put("phoneNumber", updatedUser.getPhoneNumber() != null ? updatedUser.getPhoneNumber() : "");
            userInfo.put("idProofNumber", updatedUser.getIdProofNumber() != null ? updatedUser.getIdProofNumber() : "");
            userInfo.put("address", updatedUser.getAddress() != null ? updatedUser.getAddress() : "");
            userInfo.put("partyName", updatedUser.getPartyName() != null ? updatedUser.getPartyName() : "");
            userInfo.put("userType", updatedUser.getUserType().name());
            userInfo.put("adminRole", updatedUser.getAdminRole() != null ? updatedUser.getAdminRole().name() : "");
            userInfo.put("isActive", updatedUser.getIsActive());
            userInfo.put("isVerified", updatedUser.getIsVerified());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update profile: " + e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("No user is currently authenticated.");
            }

            User updatedUser = authService.changePassword(principal.getName(), request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password updated successfully");
            response.put("email", updatedUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update password: " + e.getMessage());
        }
    }
}
