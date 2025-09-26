package com.voterow.backend.dto;

import java.time.Instant;

public class ElectionRequest {
    private String title;
    private Instant startDate;
    private Instant endDate;

    // Default constructor
    public ElectionRequest() {}

    // Constructor
    public ElectionRequest(String title, Instant startDate, Instant endDate) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
}