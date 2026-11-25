package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.repository.MembershipRepository;
import com.perkmanager.perk_manager.repository.PerkVoteRepository;
import com.perkmanager.perk_manager.repository.UserRepository;
import com.perkmanager.perk_manager.search.PerkFilterOptions;
import com.perkmanager.perk_manager.search.PerkSearchCriteria;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class PerkService {

    private final PerkRepository perkRepository;
    private final MembershipRepository membershipRepository;
    private final PerkVoteRepository voteRepository;
    private final UserRepository userRepository;

    public PerkService(
            PerkRepository perkRepository,
            MembershipRepository membershipRepository,
            PerkVoteRepository voteRepository,
            UserRepository userRepository) {

        this.perkRepository = perkRepository;
        this.membershipRepository = membershipRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    // Retrieve perks associated with a membership
    public List<Perk> getPerksByMembership(Long membershipId) {

        // Validate membership ID
        if (membershipId == null || membershipId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid membership ID"
            );
        }

        // Check whether membership exists
        if (!membershipRepository.existsById(membershipId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Membership not found"
            );
        }

        // Retrieve perks belonging to the membership
        return perkRepository.findByMembershipId(membershipId);
    }

    // Active perks matching a keyword, optionally limited to an area and to a set of memberships.
    //   keyword: null or blank matches everything (product, title, description, membership name)
    //   area: null or blank means no area filter; otherwise keeps perks with no area limit or that exact area
    //   onlyMembershipIds: null means all memberships; an empty set means no perks at all

    // Original 3-argument search, kept so MembershipController keeps working unchanged
    public List<Perk> searchActivePerks(String keyword, String area, Set<Long> onlyMembershipIds) {
        return searchActivePerks(PerkSearchCriteria.of(keyword, area, onlyMembershipIds));
    }

    // Active perks matching every option that is set in the criteria. Results keep the
    // repository's order: soonest expiry first, perks that never expire last.
    public List<Perk> searchActivePerks(PerkSearchCriteria c){
        LocalDate today = LocalDate.now();

        // Step 1: the database does the text search (and removes expired perks)
        List<Perk> candidates = c.productOnly()
                ? perkRepository.searchActiveByProduct(c.keyword(), today)
                : perkRepository.searchActive(c.keyword(), today);

        // Latest expiry date allowed by "expires within N days" (null = no limit)
        LocalDate cutoff = c.expiringWithinDays() == null ? null : today.plusDays(c.expiringWithinDays());

        // Step 2: each remaining option narrows the list further
        return candidates.stream()
                // keeps perks with no area limit, or limited to the chosen area
                .filter(p -> c.area().isEmpty()
                    || p.getGeographicArea() == null
                    || p.getGeographicArea().equalsIgnoreCase(c.area()))
                .filter(p -> c.membershipId() == null
                    || c.membershipId().equals(p.getMembership().getId()))
                .filter(p -> c.discountType() == null
                    || p.getDiscountType() == c.discountType())
                // perks that never expire (null date) don't count as "expiring soon"
                .filter(p -> cutoff == null
                    || (p.getExpiryDate() != null && !p.getExpiryDate().isAfter(cutoff)))
                .filter(p -> c.onlyMembershipIds() == null
                    || c.onlyMembershipIds().contains(p.getMembership().getId()))
                .toList();
    }

    // Choices for the page's dropdowns and autocomplete
    @Transactional(readOnly = true)
    public PerkFilterOptions filterOptions() {
        LocalDate today = LocalDate.now();
        return new PerkFilterOptions(
                membershipRepository.findAllByOrderByNameAsc(),
                perkRepository.findActiveAreas(today),
                perkRepository.findActiveProductNames(today)
        );
    }

    @Transactional
    public void castVote(Long userId, Long perkId, Short value) {
        if (userId == null || perkId == null || perkId <= 0 || value == null || (value != 1 && value != -1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an upvote or downvote.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Perk perk = perkRepository.findById(perkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perk not found."));
        voteRepository.findByUserIdAndPerkId(userId, perkId).ifPresentOrElse(
                existing -> {
                    if (existing.getValue() == value) voteRepository.delete(existing);
                    else existing.setValue(value);
                },
                () -> voteRepository.save(new PerkVote(user, perk, value)));
    }

    @Transactional
    public void removeVote(Long userId, Long perkId) {
        if (userId == null || perkId == null || perkId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vote request.");
        }
        voteRepository.findByUserIdAndPerkId(userId, perkId).ifPresent(voteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PerkVote> votesForPerks(List<Long> perkIds) {
        return perkIds.isEmpty() ? List.of() : voteRepository.findByPerkIdIn(perkIds);
    }
}
