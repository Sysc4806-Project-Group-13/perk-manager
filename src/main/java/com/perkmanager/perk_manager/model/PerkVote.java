package com.perkmanager.perk_manager.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "perk_votes")
@IdClass(PerkVoteId.class)
public class PerkVote {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perk_id", nullable = false)
    private Perk perk;

    @Column(name = "vote_value", nullable = false)
    private short value;

    protected PerkVote() { }

    public PerkVote(User user, Perk perk, short value) {
        this.user = user;
        this.perk = perk;
        setValue(value);
    }

    public User getUser() { return user; }
    public Perk getPerk() { return perk; }
    public short getValue() { return value; }
    public void setValue(short value) {
        if (value != -1 && value != 1) throw new IllegalArgumentException("Vote must be -1 or 1.");
        this.value = value;
    }
}
