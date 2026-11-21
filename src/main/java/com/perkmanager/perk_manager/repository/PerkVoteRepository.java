package com.perkmanager.perk_manager.repository;

import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.model.PerkVoteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PerkVoteRepository extends JpaRepository<PerkVote, PerkVoteId> {
    List<PerkVote> findByPerkIdIn(Collection<Long> perkIds);
    Optional<PerkVote> findByUserIdAndPerkId(Long userId, Long perkId);
}
