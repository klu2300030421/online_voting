package com.voterow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
    
    private Integer age;
    
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
    
    @Size(max = 20, message = "ID proof number must not exceed 20 characters")
    private String idProofNumber;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    // Constructors
    public UpdateProfileRequest() {}
    
    public UpdateProfileRequest(String fullName, Integer age, String phoneNumber, String idProofNumber, String address) {
        this.fullName = fullName;
        this.age = age;
        this.phoneNumber = phoneNumber;
        this.idProofNumber = idProofNumber;
        this.address = address;
    }
    
    // Getters and Setters
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getIdProofNumber() {
        return idProofNumber;
    }
    
    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
}