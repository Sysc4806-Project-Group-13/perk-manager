package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.services.UserService;
import com.perkmanager.perk_manager.model.Membership;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;


// Tests the PerkController independently of the service implementation
//  in isolation: services are mocked, no Spring context is started
class PerkControllerTest {

    private PerkService perkService;
    private UserService userService;
    private PerkController controller;
    private HttpServletRequest request;
    private Model model;
    private MockedStatic<AuthController> auth;

    @BeforeEach
    void setUp() {
        perkService = mock(PerkService.class);
        userService = mock(UserService.class);
        controller = new PerkController(perkService, userService);
        request = mock(HttpServletRequest.class);
        model = new ExtendedModelMap();

        // currentUserId(...) is a static method, so it has to be mocked statically
        auth = mockStatic(AuthController.class);
    }

    @AfterEach
    void tearDown() {
        // Static mocks are thread-local and must be closed after every test
        auth.close();
    }

    // ---------- helpers ----------

    private void loginAs(Long userId) {
        auth.when(() -> AuthController.currentUserId(request)).thenReturn(userId);
    }

    private Perk perkInMembership(Long membershipId) {
        Membership membership = mock(Membership.class);
        when(membership.getId()).thenReturn(membershipId);
        Perk perk = mock(Perk.class);
        when(perk.getMembership()).thenReturn(membership);
        return perk;
    }

    @SuppressWarnings("unchecked")
    private List<PerkController.PerkRow> rowsFromModel() {
        return (List<PerkController.PerkRow>) model.getAttribute("rows");
    }

    // ---------- JSON API: GET /api/perks ----------
    // Test that the controller returns perks for a selected membership

    @Test
    void getPerksByMembershipReturnsPerks() {
        Perk perk = mock(Perk.class);
        when(perkService.getPerksByMembership(1L)).thenReturn(List.of(perk));

        List<Perk> result = controller.getPerksByMembership(1L);

        assertEquals(1, result.size());
        verify(perkService).getPerksByMembership(1L);
    }

    @Test
    void getPerksByMembershipReturnsEmptyList() {
        when(perkService.getPerksByMembership(1L)).thenReturn(List.of());

        List<Perk> result = controller.getPerksByMembership(1L);

        assertTrue(result.isEmpty());
    }

    // ---------- HTML page: GET /perks ----------

    @Test
    void perksReturnsPerksViewName() {
        loginAs(null);
        when(perkService.searchActivePerks(any(), any(), any())).thenReturn(List.of());

        String view = controller.perks(null, null, false, request, model);

        assertEquals("perks", view);
        assertEquals("perks", model.getAttribute("activeNav"));
    }

    @Test
    void perksTrimsInputsAndTreatsNullAsEmpty() {
        loginAs(null);
        when(perkService.searchActivePerks(any(), any(), any())).thenReturn(List.of());

        // Whitespace is trimmed on q, and a null area becomes ""
        controller.perks("  coffee  ", null, false, request, model);

        verify(perkService).searchActivePerks(eq("coffee"), eq(""), isNull());
        assertEquals("coffee", model.getAttribute("q"));
        assertEquals("", model.getAttribute("area"));
    }

    @Test
    void perksForVisitorIsNotLoggedInAndNeverLooksUpMemberships() {
        loginAs(null);
        Perk perk = perkInMembership(1L);
        when(perkService.searchActivePerks("", "", null))
                .thenReturn(List.of(perk));

        // mine=true is ignored for visitors because onlyMine = mine && loggedIn
        controller.perks(null, null, true, request, model);

        assertEquals(false, model.getAttribute("loggedIn"));
        assertEquals(false, model.getAttribute("mine"));
        verifyNoInteractions(userService);
        verify(perkService).searchActivePerks("", "", null);

        // With no memberships, nothing is flagged
        assertFalse(rowsFromModel().get(0).isMatchesProfile());
    }

    @Test
    void perksForLoggedInUserFlagsMatchesButDoesNotFilterWhenMineIsFalse() {
        loginAs(5L);
        when(userService.membershipIdsForUser(5L)).thenReturn(Set.of(1L));

        Perk mine = perkInMembership(1L);
        Perk other = perkInMembership(2L);
        // null as the third argument means "don't filter by membership"
        when(perkService.searchActivePerks("", "", null)).thenReturn(List.of(mine, other));

        controller.perks(null, null, false, request, model);

        List<PerkController.PerkRow> rows = rowsFromModel();
        assertEquals(2, rows.size());
        assertSame(mine, rows.get(0).getPerk());
        assertTrue(rows.get(0).isMatchesProfile());
        assertSame(other, rows.get(1).getPerk());
        assertFalse(rows.get(1).isMatchesProfile());

        assertEquals(true, model.getAttribute("loggedIn"));
        assertEquals(false, model.getAttribute("mine"));
    }

    @Test
    void perksForLoggedInUserWithMinePassesMembershipIdsToService() {
        loginAs(5L);
        Set<Long> myIds = Set.of(1L, 3L);
        when(userService.membershipIdsForUser(5L)).thenReturn(myIds);

        Perk mine = perkInMembership(1L);
        when(perkService.searchActivePerks("tea", "ottawa", myIds)).thenReturn(List.of(mine));

        controller.perks("tea", "ottawa", true, request, model);

        verify(perkService).searchActivePerks("tea", "ottawa", myIds);
        assertEquals(true, model.getAttribute("mine"));
        assertEquals(1, rowsFromModel().size());
        assertTrue(rowsFromModel().get(0).isMatchesProfile());
    }

    /**
    @Test
    void getPerksByMembershipReturnsPerks() {

        // Create a mock service
        PerkService service = mock(PerkService.class);

        // Create the controller using the mock service
        PerkController controller =
                new PerkController(service);

        // Define the membership ID being requested
        Long membershipId = 1L;

        // Create a sample perk
        Perk perk = mock(Perk.class);

        // Configure the service to return one perk
        when(service.getPerksByMembership(membershipId))
                .thenReturn(List.of(perk));

        // Call the controller method
        List<Perk> result =
                controller.getPerksByMembership(membershipId);

        // Verify that the controller returns one perk
        assertEquals(1, result.size());

        // Verify that the controller passes the correct ID to the service
        verify(service).getPerksByMembership(membershipId);
    }

    // Test that the controller handles memberships with no perks
    @Test
    void getPerksByMembershipReturnsEmptyList() {

        // Create a mock service
        PerkService service = mock(PerkService.class);

        // Create the controller
        PerkController controller =
                new PerkController(service);

        // Configure the service to return an empty list
        when(service.getPerksByMembership(1L))
                .thenReturn(List.of());

        // Call the controller method
        List<Perk> result =
                controller.getPerksByMembership(1L);

        // Verify that no perks are returned
        assertTrue(result.isEmpty());
    } **/
}