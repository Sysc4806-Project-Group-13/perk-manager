package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

// Registration, login and logout. "Logged in" means the session holds the user's id.
@Controller
public class AuthController {
    // Names of the values stored in the HTTP session (layout.html reads them as session.userId / session.username)
    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_USERNAME = "username";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerForm(HttpServletRequest request, Model model) {
        if (currentUserId(request) != null) {
            return "redirect:/profile";
        }
        model.addAttribute("activeNav", "register");
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           HttpServletRequest request,
                           Model model) {
        try {
            User user = userService.register(username, password, confirmPassword);
            startSession(request, user); // sign the new user straight in
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formUsername", username); // keep what they typed (never the password)
            model.addAttribute("activeNav", "register");
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm(HttpServletRequest request, Model model) {
        if (currentUserId(request) != null) {
            return "redirect:/profile";
        }
        model.addAttribute("activeNav", "login");
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {
        Optional<User> user = userService.authenticate(username, password);
        if (user.isEmpty()) {
            model.addAttribute("error", "Invalid username or password.");
            model.addAttribute("formUsername", username);
            model.addAttribute("activeNav", "login");
            return "login";
        }
        startSession(request, user.get());
        return "redirect:/profile";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // do not create a session just to destroy it
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    // The logged-in user's id from the session, or null if nobody is logged in (shared by all controllers)
    public static Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(SESSION_USER_ID);
        return id instanceof Long userId ? userId : null;
    }

    // Marks the browser session as logged in as this user
    private void startSession(HttpServletRequest request, User user) {
        request.getSession(true);   // make sure a session exists
        request.changeSessionId();      // give it a new id (prevents session fixation)
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USERNAME, user.getUsername());
        session.setMaxInactiveInterval(30 * 60); // log out after 30 idle minutes
    }

}
