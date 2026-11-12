package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

// The logged-in user's profile: their memberships and the perks those memberships unlock
@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest request, Model model) {
        Long userId = AuthController.currentUserId(request);
        if (userId == null) {
            return "redirect:/login";
        }
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) { // account was deleted while the session was still alive
            request.getSession().invalidate();
            return "redirect:/login";
        }
        model.addAttribute("profileName", user.get().getUsername());
        model.addAttribute("memberships", userService.membershipsOf(userId));
        model.addAttribute("availableMemberships", userService.membershipsNotOwnedBy(userId));
        model.addAttribute("perks", userService.perksForUser(userId));
        model.addAttribute("activeNav", "profile");
        return "profile";
    }

    @PostMapping("/profile/memberships")
    public String addMembership(@RequestParam Long membershipId,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        Long userId = AuthController.currentUserId(request);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            userService.addMembership(userId, membershipId);
            redirectAttributes.addFlashAttribute("message", "Membership added to your profile.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/memberships/{membershipId}/remove")
    public String removeMembership(@PathVariable Long membershipId,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        Long userId = AuthController.currentUserId(request);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            userService.removeMembership(userId, membershipId);
            redirectAttributes.addFlashAttribute("message", "Membership removed from your profile.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

}
