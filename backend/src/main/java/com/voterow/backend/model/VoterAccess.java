package com.voterow.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voter_access")
public class VoterAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "voter_id")
    private User voter;

    @ManyToOne
    @JoinColumn(name = "election_id")
    private Election election;

    @Column(name = "access_token", unique = true)
    private String accessToken;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public VoterAccess() {}

    public VoterAccess(User voter, Election election, String accessToken) {
        this.voter = voter;
        this.election = election;
        this.accessToken = accessToken;
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }

    public Election getElection() { return election; }
    public void setElection(Election election) { this.election = election; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}