package com.voterow.backend.dto;

public class VoteRequest {
    private Long participantId;

    // Default constructor
    public VoteRequest() {}

    // Constructor
    public VoteRequest(Long participantId) {
        this.participantId = participantId;
    }

    // Getters and Setters
    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }
}