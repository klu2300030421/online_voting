package com.voterow.backend.service;

import com.voterow.backend.dto.SignUpRequest;
import com.voterow.backend.dto.ChangePasswordRequest;
import com.voterow.backend.dto.UpdateProfileRequest;
import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    // These must be final for RequiredArgsConstructor to initialize them
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(SignUpRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalStateException("Email already in use");
        });

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Handle user type mapping from frontend to backend
        String userTypeStr = request.getUserType().toUpperCase();
        
        // Map frontend user types to backend enum values
        UserType userType;
        switch (userTypeStr) {
            case "VOTER":
                userType = UserType.ROLE_VOTER;
                break;
            case "PARTICIPANT":
            case "CANDIDATE": // Support both "Participant" and "Candidate" from frontend
                userType = UserType.ROLE_PARTICIPANT;
                break;
            case "ADMIN":
                throw new IllegalArgumentException("Administrator accounts cannot be created through public signup");
            default:
                throw new IllegalArgumentException("Invalid user type: " + userTypeStr);
        }
        
        user.setUserType(userType);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsActive(true);
        user.setIsVerified(false);
        user.setTwoFactorEnabled(false);
        
        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User updateLastLogin(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }

    public User createAdminUser(String email, String password, String fullName) {
        // Check if admin already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Admin user already exists with email: " + email);
        }

        User admin = new User();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setAge(30);
        admin.setUserType(UserType.ROLE_ADMIN);
        admin.setAdminRole(com.voterow.backend.model.AdminRole.SUPER_ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setIsActive(true);
        admin.setIsVerified(true);
        admin.setTwoFactorEnabled(false);
        
        return userRepository.save(admin);
    }

    public User updateUserProfile(String email, UpdateProfileRequest updateRequest) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Update profile fields
        if (updateRequest.getFullName() != null && !updateRequest.getFullName().trim().isEmpty()) {
            user.setFullName(updateRequest.getFullName().trim());
        }
        
        if (updateRequest.getAge() != null && updateRequest.getAge() > 0) {
            user.setAge(updateRequest.getAge());
        }
        
        if (updateRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(updateRequest.getPhoneNumber().trim().isEmpty() ? null : updateRequest.getPhoneNumber().trim());
        }
        
        if (updateRequest.getIdProofNumber() != null) {
            user.setIdProofNumber(updateRequest.getIdProofNumber().trim().isEmpty() ? null : updateRequest.getIdProofNumber().trim());
        }
        
        if (updateRequest.getAddress() != null) {
            user.setAddress(updateRequest.getAddress().trim().isEmpty() ? null : updateRequest.getAddress().trim());
        }
        
        if (updateRequest.getPartyName() != null) {
            user.setPartyName(updateRequest.getPartyName().trim().isEmpty() ? null : updateRequest.getPartyName().trim());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    public User changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
