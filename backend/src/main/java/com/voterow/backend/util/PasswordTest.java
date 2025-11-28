package com.voterow.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        String encoded = encoder.encode(password);
        
        System.out.println("Original password: " + password);
        System.out.println("Encoded password: " + encoded);
        System.out.println("Verification: " + encoder.matches(password, encoded));
    }
}