package com.voterow.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "election_voters")
public class ElectionVoter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "election_id")
    private Election election;
    
    @ManyToOne
    @JoinColumn(name = "voter_id")
    private User voter;
    
    @Column(name = "registered_at")
    private LocalDateTime registeredAt = LocalDateTime.now();
    
    @Column(name = "is_eligible")
    private Boolean isEligible = true;

    // Constructors
    public ElectionVoter() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Election getElection() { return election; }
    public void setElection(Election election) { this.election = election; }
    
    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }
    
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    
    public Boolean getIsEligible() { return isEligible; }
    public void setIsEligible(Boolean isEligible) { this.isEligible = isEligible; }
}