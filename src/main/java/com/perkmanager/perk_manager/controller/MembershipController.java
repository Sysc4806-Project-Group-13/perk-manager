
package com.perkmanager.perk_manager.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.services.MembershipService;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    // Constructor injection
    public MembershipController(
            MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // GET /api/memberships
    @GetMapping
    public List<Membership> getAllMemberships() {
        return membershipService.getAllMemberships();
    }
}
