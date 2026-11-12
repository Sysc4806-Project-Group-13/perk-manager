package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.repository.MembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class PerkService {

    private final PerkRepository perkRepository;
    private final MembershipRepository membershipRepository;

    public PerkService(
            PerkRepository perkRepository,
            MembershipRepository membershipRepository) {

        this.perkRepository = perkRepository;
        this.membershipRepository = membershipRepository;
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
    public List<Perk> searchActivePerks(String keyword, String area, Set<Long> onlyMembershipIds) {
        String q = keyword == null ? "" : keyword.trim();
        String wantedArea = area == null ? "" : area.trim();

        return perkRepository.searchActive(q, LocalDate.now()).stream()
                .filter(p -> wantedArea.isEmpty()
                        || p.getGeographicArea() == null
                        || p.getGeographicArea().equalsIgnoreCase(wantedArea))
                .filter(p -> onlyMembershipIds == null
                        || onlyMembershipIds.contains(p.getMembership().getId()))
                .toList();
    }
}