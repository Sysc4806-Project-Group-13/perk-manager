package com.perkmanager.perk_manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

// User account entity
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // The memberships in this user's profile. Rows live in the user_memberships join table.
    @ManyToMany
    @JoinTable(name = "user_memberships",
            // column pointing at this entity
            joinColumns = @JoinColumn(name = "user_id"),
            // column pointing at the other side/entity
            inverseJoinColumns = @JoinColumn(name = "membership_id"))
    private Set<Membership> memberships = new HashSet<>();

    // Required by JPA
    public User() { }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    // Set avoids duplicate memberships for a user
    // with this removing one membership deletes exactly one join-table row
    public Set<Membership> getMemberships() { return memberships; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }

    // Deliberately leaves out passwordHash and memberships
    @Override
    public String toString() {
        return "User {id= " + id + ", username= '" + username + "'}";
    }
}
