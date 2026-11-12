package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Set;


// Perks: an HTML search page (/perks) and a JSON endpoint (/api/perks)
@Controller
public class PerkController {

    private final PerkService perkService;
    private final UserService userService;

    // Constructor injection
    public PerkController(PerkService perkService, UserService userService) {
        this.perkService = perkService;
        this.userService = userService;
    }

    // ---------- JSON API ----------

    // GET /api/perks?membershipId=1
    @GetMapping("/api/perks")
    @ResponseBody
    public List<Perk> getPerksByMembership(@RequestParam Long membershipId) {
        return perkService.getPerksByMembership(membershipId);
    }

    // ---------- HTML page ----------

    // GET /perks : search active perks by keyword and area, soonest expiry first.
    // Visitors can search; logged-in users also get matches flagged and an "only my memberships" filter.
    @GetMapping("/perks")
    public String perks(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String area,
                        @RequestParam(defaultValue = "false") boolean mine,
                        HttpServletRequest request,
                        Model model) {
        String keyword = q == null ? "" : q.trim();
        String areaFilter = area == null ? "" : area.trim();

        Long userId = AuthController.currentUserId(request);
        boolean loggedIn = userId != null;
        Set<Long> myIds = loggedIn ? userService.membershipIdsForUser(userId) : Set.of();
        // Essentially a toggle
        boolean onlyMine = mine && loggedIn;

        List<PerkRow> rows = perkService.searchActivePerks(keyword, areaFilter, onlyMine ? myIds : null).stream()
                .map(p -> new PerkRow(p, myIds.contains(p.getMembership().getId())))
                .toList();

        model.addAttribute("rows", rows);
        model.addAttribute("q", keyword);
        model.addAttribute("area", areaFilter);
        model.addAttribute("mine", onlyMine);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("activeNav", "perks");
        return "perks";
    }

    // One row on the page: a perk, and whether it belongs to one of the viewer's memberships
    public static class PerkRow {
        private final Perk perk;
        private final boolean matchesProfile;

        public PerkRow(Perk perk, boolean matchesProfile) {
            this.perk = perk;
            this.matchesProfile = matchesProfile;
        }

        public Perk getPerk() { return perk; }
        public boolean isMatchesProfile() { return matchesProfile; }
    }
}
