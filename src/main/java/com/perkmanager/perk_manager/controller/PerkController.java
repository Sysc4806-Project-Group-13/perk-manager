package com.perkmanager.perk_manager.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.services.PerkService;

@RestController
@RequestMapping("/api/perks")
public class PerkController {

    private final PerkService perkService;

    // Constructor injection
    public PerkController(PerkService perkService) {
        this.perkService = perkService;
    }

    // GET /api/perks?membershipId=1
    @GetMapping
    public List<Perk> getPerksByMembership(
            @RequestParam Long membershipId) {

        return perkService.getPerksByMembership(membershipId);
    }
}
