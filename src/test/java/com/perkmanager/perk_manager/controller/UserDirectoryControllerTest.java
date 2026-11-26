package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.services.UserService;
import com.perkmanager.perk_manager.services.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Tests UserDirectoryController in isolation: the service is mocked, no Spring context is started
class UserDirectoryControllerTest {

    private UserService userService;
    private UserDirectoryController controller;
    private HttpServletRequest request;
    private ExtendedModelMap model;
    private RedirectAttributesModelMap redirect;
    private MockedStatic<AuthController> auth;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserDirectoryController(userService);
        request = mock(HttpServletRequest.class);
        model = new ExtendedModelMap();
        redirect = new RedirectAttributesModelMap();
        auth = mockStatic(AuthController.class);
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    // Helper: pretend this user id is logged in (null means a visitor who is not logged in)
    private void loginAs(Long userId) {
        auth.when(() -> AuthController.currentUserId(request)).thenReturn(userId);
    }

    // A visitor who is not logged in can still see the list of users.
    @Test
    void usersListShowsEveryUserToVisitors() {
        loginAs(null);
        List<UserSummary> summaries = List.of(new UserSummary(1L, "alex", 2), new UserSummary(2L, "sam", 0));
        when(userService.listUserSummaries()).thenReturn(summaries);

        String view = controller.users(request, model);

        assertEquals("users", view);
        assertSame(summaries, model.getAttribute("users"));
        assertNull(model.getAttribute("currentUserId"));
        assertEquals("users", model.getAttribute("activeNav"));
    }

    // The page gets the logged-in user's id so it can show a "You" badge next to their name.
    @Test
    void usersListKnowsWhoIsLoggedIn() {
        loginAs(5L);
        when(userService.listUserSummaries()).thenReturn(List.of());

        controller.users(request, model);

        assertEquals(5L, model.getAttribute("currentUserId"));
    }

    // Opening another user's page shows their username and memberships, and self is false.
    @Test
    void userPageShowsUsernameAndMemberships() {
        loginAs(null);
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("alex");
        when(userService.findById(3L)).thenReturn(Optional.of(user));
        List<Membership> memberships = List.of(mock(Membership.class));
        when(userService.membershipsOf(3L)).thenReturn(memberships);

        String view = controller.user(3L, request, model, redirect);

        assertEquals("user", view);
        assertEquals("alex", model.getAttribute("profileName"));
        assertSame(memberships, model.getAttribute("memberships"));
        assertEquals(false, model.getAttribute("self"));
    }

    // Opening your own page sets self to true so the page can link to your profile.
    @Test
    void userPageMarksYourOwnPage() {
        loginAs(3L);
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("alex");
        when(userService.findById(3L)).thenReturn(Optional.of(user));
        when(userService.membershipsOf(3L)).thenReturn(List.of());

        controller.user(3L, request, model, redirect);

        assertEquals(true, model.getAttribute("self"));
    }

    // An id that does not exist sends the visitor back to the list with an error message
    // instead of crashing, and never tries to load memberships.
    @Test
    void unknownUserRedirectsToTheListWithAnError() {
        loginAs(null);
        when(userService.findById(99L)).thenReturn(Optional.empty());

        String view = controller.user(99L, request, model, redirect);

        assertEquals("redirect:/users", view);
        assertEquals("That user does not exist.", redirect.getFlashAttributes().get("error"));
        verify(userService, never()).membershipsOf(any());
    }
}