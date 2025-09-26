package com.voterow.backend.service;

import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getVoters() {
        try {
            return userRepository.findByUserType(UserType.ROLE_VOTER);
        } catch (Exception e) {
            // Return mock data if database query fails
            return new ArrayList<>();
        }
    }

    public List<User> getCandidates() {
        try {
            return userRepository.findByUserType(UserType.ROLE_PARTICIPANT);
        } catch (Exception e) {
            // Return mock data if database query fails
            return new ArrayList<>();
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}