package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileControllerTest {
    private UserService userService;
    private PerkService perkService;
    private ProfileController controller;
    private HttpServletRequest request;
    private ExtendedModelMap model;
    private MockedStatic<AuthController> auth;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        perkService = mock(PerkService.class);
        controller = new ProfileController(userService, perkService);
        request = mock(HttpServletRequest.class);
        model = new ExtendedModelMap();
        auth = mockStatic(AuthController.class);
        auth.when(() -> AuthController.currentUserId(request)).thenReturn(8L);
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    @Test
    void profileDisplaysVoteScoreAndCurrentUsersVoteForMembershipPerks() {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("alex");
        when(userService.findById(8L)).thenReturn(Optional.of(user));
        when(userService.membershipsOf(8L)).thenReturn(List.of());
        when(userService.membershipsNotOwnedBy(8L)).thenReturn(List.of());

        Perk perk = mock(Perk.class);
        when(perk.getId()).thenReturn(12L);
        when(userService.perksForUser(8L)).thenReturn(List.of(perk));

        User voter = mock(User.class);
        when(voter.getId()).thenReturn(8L);
        PerkVote mine = new PerkVote(voter, perk, (short) 1);
        User otherVoter = mock(User.class);
        when(otherVoter.getId()).thenReturn(9L);
        PerkVote other = new PerkVote(otherVoter, perk, (short) -1);
        when(perkService.votesForPerks(List.of(12L))).thenReturn(List.of(mine, other));

        String view = controller.profile(request, model);

        assertEquals("profile", view);
        @SuppressWarnings("unchecked")
        List<PerkController.PerkRow> rows = (List<PerkController.PerkRow>) model.getAttribute("perkRows");
        assertEquals(1, rows.size());
        assertSame(perk, rows.get(0).getPerk());
        assertEquals(0, rows.get(0).getVoteScore());
        assertEquals((short) 1, rows.get(0).getMyVote());
    }
}
