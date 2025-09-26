package com.voterow.backend.model;

public enum AdminRole {
    SUPER_ADMIN("Super Admin"),
    SUB_ADMIN("Sub Admin"),
    ELECTION_MANAGER("Election Manager"),
    VOTER_MANAGER("Voter Manager");

    private final String displayName;

    AdminRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}