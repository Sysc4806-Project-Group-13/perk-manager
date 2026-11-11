
package com.perkmanager.perk_manager.services;

import org.springframework.stereotype.Service;
import java.util.List;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.repository.MembershipRepository;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    // Constructor injection
    public MembershipService(
            MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    // Retrieve all available memberships
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }
}
