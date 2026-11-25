package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Tests the success messages shown after register, login and logout.
// The service is mocked, so no Spring context or database is started.
class AuthControllerMessageTest {

    private UserService userService;
    private AuthController controller;
    private HttpServletRequest request;
    private HttpSession session;
    private ExtendedModelMap model;
    private RedirectAttributesModelMap redirect;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new AuthController(userService);
        request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
        // the controller asks the request for a session in several ways, so all of them return the mock
        when(request.getSession()).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        model = new ExtendedModelMap();
        redirect = new RedirectAttributesModelMap();
    }

    // Helper: a mocked user with an id and a username
    private User user(Long id, String username) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    // A new account is signed in and the profile page greets them.
    @Test
    void registerShowsAWelcomeMessage() {
        User alex = user(1L, "alex");
        when(userService.register("alex", "pw", "pw")).thenReturn(alex);

        String view = controller.register("alex", "pw", "pw", request, model, redirect);

        assertEquals("redirect:/profile", view);
        assertEquals("Account created. Welcome, alex.", redirect.getFlashAttributes().get("message"));
    }

    // A failed registration shows the error on the same page and no success message.
    @Test
    void failedRegisterShowsTheErrorAndNoSuccessMessage() {
        when(userService.register("alex", "pw", "other")).thenThrow(new IllegalArgumentException("Passwords do not match."));

        String view = controller.register("alex", "pw", "other", request, model, redirect);

        assertEquals("register", view);
        assertEquals("Passwords do not match.", model.getAttribute("error"));
        assertNull(redirect.getFlashAttributes().get("message"));
    }

    // A successful login redirects to the profile with a "welcome back" message.
    @Test
    void loginShowsAWelcomeBackMessage() {
        User alex = user(1L, "alex");
        when(userService.authenticate("alex", "pw")).thenReturn(Optional.of(alex));

        String view = controller.login("alex", "pw", request, model, redirect);

        assertEquals("redirect:/profile", view);
        assertEquals("Welcome back, alex.", redirect.getFlashAttributes().get("message"));
    }

    // A wrong password stays on the login page with an error and no success message.
    @Test
    void failedLoginShowsTheErrorAndNoSuccessMessage() {
        when(userService.authenticate("alex", "bad")).thenReturn(Optional.empty());

        String view = controller.login("alex", "bad", request, model, redirect);

        assertEquals("login", view);
        assertEquals("Invalid username or password.", model.getAttribute("error"));
        assertNull(redirect.getFlashAttributes().get("message"));
    }

    // Logging out ends the session and tells the user on the home page.
    @Test
    void logoutShowsALoggedOutMessage() {
        String view = controller.logout(request, redirect);

        assertEquals("redirect:/", view);
        verify(session).invalidate();
        assertEquals("You have been logged out.", redirect.getFlashAttributes().get("message"));
    }
}