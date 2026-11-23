package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.services.UserService;
import com.perkmanager.perk_manager.form.PerkForm;
import com.perkmanager.perk_manager.model.DiscountType;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

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

    // Display the perk creation form.
    @GetMapping("/perks/new")
    public String newPerk(
            HttpServletRequest request,
            Model model) {

        // Only logged-in users can create perks.
        if (AuthController.currentUserId(request) == null) {
            return "redirect:/login";
        }

        model.addAttribute("perkForm", new PerkForm());

        model.addAttribute(
                "memberships",
                perkService.getAllMemberships());

        model.addAttribute(
                "discountTypes",
                DiscountType.values());

        model.addAttribute("activeNav", "perks");

        return "perk-form";
    }

    // Receive and process a new perk submission.
    @PostMapping("/perks")
    public String createPerk(
            @Valid @ModelAttribute("perkForm") PerkForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Require authentication.
        if (AuthController.currentUserId(request) == null) {
            return "redirect:/login";
        }

        // Free items do not require a numeric discount value.
        if (form.getDiscountType() == DiscountType.FREE_ITEM) {
            form.setDiscountValue(null);
        }

        // Validate percentage discounts.
        if (form.getDiscountType() == DiscountType.PERCENTAGE
                && form.getDiscountValue() != null
                && form.getDiscountValue().compareTo(
                new java.math.BigDecimal("100")) > 0) {

            bindingResult.rejectValue(
                    "discountValue",
                    "invalid",
                    "Percentage discounts cannot exceed 100%."
            );
        }

        // Return to the form if validation fails.
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "memberships",
                    perkService.getAllMemberships());

            model.addAttribute(
                    "discountTypes",
                    DiscountType.values());

            model.addAttribute("activeNav", "perks");

            return "perk-form";
        }

        try {
            perkService.createPerk(form);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your perk has been successfully posted!");

            return "redirect:/perks";

        } catch (IllegalArgumentException e) {

            bindingResult.reject("perk.error", e.getMessage());

            model.addAttribute(
                    "memberships",
                    perkService.getAllMemberships());

            model.addAttribute(
                    "discountTypes",
                    DiscountType.values());

            model.addAttribute("activeNav", "perks");

            return "perk-form";
        }
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

        model.addAttribute("rows", rows);
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
