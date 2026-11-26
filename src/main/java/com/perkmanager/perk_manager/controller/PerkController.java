package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.services.UserService;
import com.perkmanager.perk_manager.model.DiscountType;
import com.perkmanager.perk_manager.search.PerkSearchCriteria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;
import java.util.Locale;


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

    // GET /perks : search active perks by product/keyword and narrow them with filters.
    // Visitors can search; logged-in users also get matches flagged and an "only my memberships" filter.
    @GetMapping("/perks")
    public String perks(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean productOnly,
                        @RequestParam(required = false) String area,
                        @RequestParam(required = false) Long membershipId,
                        @RequestParam(required = false) String discountType,
                        @RequestParam(required = false) Integer expiring,
                        @RequestParam(defaultValue = "false") boolean mine,
                        HttpServletRequest request,
                        Model model) {

        Long userId = AuthController.currentUserId(request);
        boolean loggedIn = userId != null;
        Set<Long> myIds = loggedIn ? userService.membershipIdsForUser(userId) : Set.of();
        // Essentially a toggle
        boolean onlyMine = mine && loggedIn;

        // Raw request values -> one criteria object (it trims and normalizes them)
        PerkSearchCriteria criteria = new PerkSearchCriteria(q, productOnly, area, membershipId,
                parseDiscountType(discountType), expiring, onlyMine ? myIds : null);

        List<Perk> perks = perkService.searchActivePerks(criteria);
        var votes = perkService.votesForPerks(perks.stream().map(Perk::getId).toList());
        List<PerkRow> rows = perks.stream().map(p -> {
            int score = 0;
            Short myVote = null;
            for (PerkVote vote : votes) {
                if (vote.getPerk().getId().equals(p.getId())) {
                    score += vote.getValue();
                    if (loggedIn && java.util.Objects.equals(vote.getUser().getId(), userId)) myVote = vote.getValue();
                }
            }
            return new PerkRow(p, myIds.contains(p.getMembership().getId()), score, myVote);
        }).toList();

        // Echo the chosen values back so the form keeps showing them
        model.addAttribute("rows", rows);
        model.addAttribute("q", criteria.keyword());
        model.addAttribute("productOnly", productOnly);
        model.addAttribute("area", criteria.area());
        model.addAttribute("membershipId", membershipId);
        model.addAttribute("discountType", criteria.discountType());
        model.addAttribute("expiring", criteria.expiringWithinDays());
        model.addAttribute("mine", onlyMine);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("filtersActive", criteria.hasFilters());
        // Dropdown contents
        model.addAttribute("options", perkService.filterOptions());
        model.addAttribute("discountTypes", DiscountType.values());

        model.addAttribute("activeNav", "perks");
        return "perks";
    }

    // A bad benefit type (e.g. typed into the URL) is ignored instead of causing an error
    private static DiscountType parseDiscountType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DiscountType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @PostMapping("/perks/{perkId}/vote")
    public String vote(@PathVariable Long perkId, @RequestParam Short value, HttpServletRequest request) {
        Long userId = AuthController.currentUserId(request);
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Log in to vote.");
        perkService.castVote(userId, perkId, value);
        return "redirect:/perks";
    }

    @PostMapping("/perks/{perkId}/vote/remove")
    public String removeVote(@PathVariable Long perkId, HttpServletRequest request) {
        Long userId = AuthController.currentUserId(request);
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Log in to vote.");
        perkService.removeVote(userId, perkId);
        return "redirect:/perks";
    }

    // One row on the page: a perk, and whether it belongs to one of the viewer's memberships
    public static class PerkRow {
        private final Perk perk;
        private final boolean matchesProfile;
        private final int voteScore;
        private final Short myVote;

        public PerkRow(Perk perk, boolean matchesProfile, int voteScore, Short myVote) {
            this.perk = perk;
            this.matchesProfile = matchesProfile;
            this.voteScore = voteScore;
            this.myVote = myVote;
        }

        public Perk getPerk() { return perk; }
        public boolean isMatchesProfile() { return matchesProfile; }
        public int getVoteScore() { return voteScore; }
        public Short getMyVote() { return myVote; }
    }
}
