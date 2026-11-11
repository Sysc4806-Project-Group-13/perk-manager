package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.repository.MembershipRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(
            MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    // Retrieve all memberships alphabetically
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAllByOrderByNameAsc();
    }
}