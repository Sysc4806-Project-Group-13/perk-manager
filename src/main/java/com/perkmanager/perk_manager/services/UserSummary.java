package com.perkmanager.perk_manager.services;

// One row of the "all users" page: who they are and how many memberships they have.
// Deliberately has no password hash or other private data.
public class UserSummary {
    private final Long id;
    private final String username;
    private final int membershipCount;

    public UserSummary(Long id, String username, int membershipCount) {
        this.id = id;
        this.username = username;
        this.membershipCount = membershipCount;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public int getMembershipCount() { return membershipCount; }
}