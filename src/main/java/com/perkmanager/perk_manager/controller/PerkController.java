package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.services.UserService;
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
import java.util.Comparator;

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

    // Sort options for the perks page
    public static final String SORT_EXPIRY = "expiry";
    public static final String SORT_VOTES = "votes";

    // Same page without a sort choice: soonest expiry first
    public String perks(String q, String area, boolean mine, HttpServletRequest request, Model model) {
        return perks(q, area, mine, SORT_EXPIRY, request, model);
    }

    // GET /perks : search active perks by keyword and area, sorted by soonest expiry (default) or by votes.
    @GetMapping("/perks")
    public String perks(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String area,
                        @RequestParam(defaultValue = "false") boolean mine,
                        @RequestParam(defaultValue = SORT_EXPIRY) String sort,
                        HttpServletRequest request,
                        Model model) {
        String keyword = q == null ? "" : q.trim();
        String areaFilter = area == null ? "" : area.trim();

        Long userId = AuthController.currentUserId(request);
        boolean loggedIn = userId != null;
        Set<Long> myIds = loggedIn ? userService.membershipIdsForUser(userId) : Set.of();
        // Essentially a toggle
        boolean onlyMine = mine && loggedIn;

        List<Perk> perks = perkService.searchActivePerks(keyword, areaFilter, onlyMine ? myIds : null);
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

        // Highest score first. sorted() is stable, so ties keep the expiry order from the search.
        boolean byVotes = SORT_VOTES.equals(sort);
        if (byVotes) {
            rows = rows.stream()
                    .sorted(Comparator.comparingInt(PerkRow::getVoteScore).reversed())
                    .toList();
        }

        model.addAttribute("rows", rows);
        model.addAttribute("sort", byVotes ? SORT_VOTES : SORT_EXPIRY);

        model.addAttribute("q", keyword);
        model.addAttribute("area", areaFilter);
        model.addAttribute("mine", onlyMine);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("activeNav", "perks");
        return "perks";
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
