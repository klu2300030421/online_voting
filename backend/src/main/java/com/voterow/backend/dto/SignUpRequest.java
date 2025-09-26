package com.voterow.backend.dto;

import jakarta.validation.constraints.*;

public class SignUpRequest {
    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8)
    private String password;

    @Min(18)
    private int age;

    @NotBlank
    private String userType; // "VOTER", "PARTICIPANT", or "ADMIN"

    // Default constructor
    public SignUpRequest() {}

    // Constructor
    public SignUpRequest(String fullName, String email, String password, int age, String userType) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.age = age;
        this.userType = userType;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}