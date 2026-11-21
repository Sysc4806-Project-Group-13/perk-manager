package com.perkmanager.perk_manager.model;

import java.io.Serializable;
import java.util.Objects;

public class PerkVoteId implements Serializable {
    private Long user;
    private Long perk;

    public PerkVoteId() { }

    public PerkVoteId(Long user, Long perk) {
        this.user = user;
        this.perk = perk;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerkVoteId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(perk, that.perk);
    }

    @Override public int hashCode() { return Objects.hash(user, perk); }
}
