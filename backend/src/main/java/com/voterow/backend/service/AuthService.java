package com.voterow.backend.service;

import com.voterow.backend.dto.SignUpRequest;
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
                userType = UserType.ROLE_ADMIN;
                break;
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
        
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
}