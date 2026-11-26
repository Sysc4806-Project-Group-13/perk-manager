package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

// Viewing other users: a list of everyone (/users) and one user's public page (/users/{id}).
// Anyone can look. Only the username and memberships are shown, never passwords.
@Controller
public class UserDirectoryController {

    private final UserService userService;

    public UserDirectoryController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String users(HttpServletRequest request, Model model) {
        model.addAttribute("users", userService.listUserSummaries());
        model.addAttribute("currentUserId", AuthController.currentUserId(request));
        model.addAttribute("activeNav", "users");
        return "users";
    }

    @GetMapping("/users/{userId}")
    public String user(@PathVariable Long userId,
                       HttpServletRequest request,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "That user does not exist.");
            return "redirect:/users";
        }
        model.addAttribute("profileName", user.get().getUsername());
        model.addAttribute("memberships", userService.membershipsOf(userId));
        model.addAttribute("self", userId.equals(AuthController.currentUserId(request)));
        model.addAttribute("activeNav", "users");
        return "user";
    }
}