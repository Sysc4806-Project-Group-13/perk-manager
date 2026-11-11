package com.perkmanager.perk_manager.services;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.repository.MembershipRepository;

@Service
public class PerkService {

    private final PerkRepository perkRepository;
    private final MembershipRepository membershipRepository;

    // Constructor injection
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

        // Retrieve matching perks
        return perkRepository.findByMembership_MembershipId(
                membershipId
        );
    }
}
