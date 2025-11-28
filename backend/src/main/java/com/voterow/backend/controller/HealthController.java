package com.voterow.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Backend is running");
        response.put("timestamp", java.time.LocalDateTime.now());
        return response;
    }
    
    @GetMapping("/test-admin")
    public Map<String, Object> testAdmin() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test admin user: admin@test.com / admin123");
        response.put("loginUrl", "POST /api/auth/login");
        return response;
    }
}