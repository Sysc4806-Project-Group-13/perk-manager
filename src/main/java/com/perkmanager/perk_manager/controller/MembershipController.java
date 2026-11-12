package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.services.MembershipService;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MembershipController {

    private final MembershipService membershipService;
    private final PerkService perkService;
    private final UserService userService;

    // Constructor injection
    public MembershipController(MembershipService membershipService, PerkService perkService, UserService userService) {
        this.membershipService = membershipService;
        this.perkService = perkService;
        this.userService = userService;
    }

    // ---------- JSON API ----------
    // GET /api/memberships
    @GetMapping("/api/memberships")
    @ResponseBody
    public List<Membership> getAllMemberships() {
        return membershipService.getAllMemberships();
    }

    // ---------- HTML page ----------

    // GET /memberships : browse memberships and their active perks (visitors and logged-in users)
    @GetMapping("/memberships")
    public String memberships(@RequestParam(required = false) String q,
                              @RequestParam(defaultValue = "false") boolean mine,
                              HttpServletRequest request,
                              Model model) {
        String keyword = q == null ? "" : q.trim();
        boolean searching = !keyword.isEmpty();

        Long userId = AuthController.currentUserId(request);
        boolean loggedIn = userId != null;
        Set<Long> myIds = loggedIn ? userService.membershipIdsForUser(userId) : Set.of();
        // Essentially a toggle
        boolean onlyMine = mine && loggedIn;

        // Every active perk matching the keyword, grouped by the membership it belongs to
        Map<Long, List<Perk>> perksByMembership = perkService.searchActivePerks(keyword, null, null).stream()
                .collect(Collectors.groupingBy(p -> p.getMembership().getId()));

        List<MembershipPerks> rows = membershipService.getAllMemberships().stream()
                // keep m if onlyMine toggle is off, or if the membership id is in the user's set of ids
                .filter(m -> !onlyMine || myIds.contains(m.getId()))
                .map(m -> new MembershipPerks(m,
                        perksByMembership.getOrDefault(m.getId(), List.of()),
                        myIds.contains(m.getId())))
                // keeps a row if no search is active, or the row has at least one perk
                .filter(row -> !searching || !row.getPerks().isEmpty())
                .toList();

        model.addAttribute("rows", rows);
        model.addAttribute("q", keyword);
        model.addAttribute("mine", onlyMine);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("activeNav", "memberships");
        return "memberships";
    }

    // One row on the page: a membership, its active perks, and whether the viewer has it
    public static class MembershipPerks {
        private final Membership membership;
        private final List<Perk> perks;
        private final boolean mine;

        public MembershipPerks(Membership membership, List<Perk> perks, boolean mine) {
            this.membership = membership;
            this.perks = perks;
            this.mine = mine;
        }

        public Membership getMembership() { return membership; }
        public List<Perk> getPerks() { return perks; }
        public boolean isMine() { return mine; }
    }

}