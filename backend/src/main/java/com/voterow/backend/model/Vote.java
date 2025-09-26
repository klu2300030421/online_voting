package com.voterow.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    
    @Column(nullable = false)
    private LocalDateTime votedAt = LocalDateTime.now();
    
    @Column(length = 45)
    private String voterIpAddress;
    
    @Column(length = 500)
    private String voterUserAgent;
    
    @Column(nullable = false, length = 500)
    private String encryptedVote; // Encrypted vote data for security
    
    @Column(nullable = false)
    private Boolean isValid = true;
    
    @Column(length = 500)
    private String invalidationReason;
    
    private LocalDateTime invalidatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invalidated_by")
    private User invalidatedBy;

    // Default constructor
    public Vote() {}

    // All args constructor
    public Vote(Long id, Election election, User voter, Candidate candidate, 
               LocalDateTime votedAt, String voterIpAddress, String voterUserAgent,
               String encryptedVote, Boolean isValid, String invalidationReason,
               LocalDateTime invalidatedAt, User invalidatedBy) {
        this.id = id;
        this.election = election;
        this.voter = voter;
        this.candidate = candidate;
        this.votedAt = votedAt;
        this.voterIpAddress = voterIpAddress;
        this.voterUserAgent = voterUserAgent;
        this.encryptedVote = encryptedVote;
        this.isValid = isValid;
        this.invalidationReason = invalidationReason;
        this.invalidatedAt = invalidatedAt;
        this.invalidatedBy = invalidatedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public User getVoter() {
        return voter;
    }

    public void setVoter(User voter) {
        this.voter = voter;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }

    public String getVoterIpAddress() {
        return voterIpAddress;
    }

    public void setVoterIpAddress(String voterIpAddress) {
        this.voterIpAddress = voterIpAddress;
    }

    public String getVoterUserAgent() {
        return voterUserAgent;
    }

    public void setVoterUserAgent(String voterUserAgent) {
        this.voterUserAgent = voterUserAgent;
    }

    public String getEncryptedVote() {
        return encryptedVote;
    }

    public void setEncryptedVote(String encryptedVote) {
        this.encryptedVote = encryptedVote;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public String getInvalidationReason() {
        return invalidationReason;
    }

    public void setInvalidationReason(String invalidationReason) {
        this.invalidationReason = invalidationReason;
    }

    public LocalDateTime getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(LocalDateTime invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

    public User getInvalidatedBy() {
        return invalidatedBy;
    }

    public void setInvalidatedBy(User invalidatedBy) {
        this.invalidatedBy = invalidatedBy;
    }
}