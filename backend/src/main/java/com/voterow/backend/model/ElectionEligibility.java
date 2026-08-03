package com.voterow.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "election_eligibility")
public class ElectionEligibility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "election_id")
    private Election election;

    @Column(name = "min_age")
    private Integer minAge = 18;

    @Column(name = "max_age")
    private Integer maxAge = 120;

    @Column(name = "required_verification")
    private Boolean requiresVerification = true;

    @Column(name = "allowed_user_types")
    private String allowedUserTypes = "ROLE_VOTER";

    @Column(name = "geographic_restriction")
    private String geographicRestriction;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public ElectionEligibility() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Election getElection() { return election; }
    public void setElection(Election election) { this.election = election; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public Boolean getRequiresVerification() { return requiresVerification; }
    public void setRequiresVerification(Boolean requiresVerification) { this.requiresVerification = requiresVerification; }

    public String getAllowedUserTypes() { return allowedUserTypes; }
    public void setAllowedUserTypes(String allowedUserTypes) { this.allowedUserTypes = allowedUserTypes; }

    public String getGeographicRestriction() { return geographicRestriction; }
    public void setGeographicRestriction(String geographicRestriction) { this.geographicRestriction = geographicRestriction; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}