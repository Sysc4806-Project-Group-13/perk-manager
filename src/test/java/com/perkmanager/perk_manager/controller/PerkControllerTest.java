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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.perkmanager.perk_manager.model.PerkVote;


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
        lenient().when(perkService.votesForPerks(anyList())).thenReturn(List.of());
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

    @Test
    void voteRequiresLoginAndDelegatesVote() {
        loginAs(42L);

        String result = controller.vote(15L, (short) 1, request);

        assertEquals("redirect:/perks", result);
        verify(perkService).castVote(42L, 15L, (short) 1);
    }

    @Test
    void voteRejectsVisitors() {
        loginAs(null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.vote(15L, (short) -1, request));
        verify(perkService, never()).castVote(any(), any(), any());
    }

    @Test
    void removeVoteRequiresLoginAndDelegatesToService() {
        loginAs(42L);

        String result = controller.removeVote(15L, request);

        assertEquals("redirect:/perks", result);
        verify(perkService).removeVote(42L, 15L);
    }
    // ---------- sorting: GET /perks?sort=... ----------
    // These tests check how the /perks page orders its rows. The service is mocked, so the search
    // always returns perks in "soonest expiry first" order, and each test only checks what the
    // controller does with that list when a sort option is chosen.

    // Helper: a mocked perk with a given id, so the controller can match votes to perks
    private Perk perkWithId(Long perkId) {
        Perk perk = perkInMembership(1L);
        when(perk.getId()).thenReturn(perkId);
        return perk;
    }
    // Helper: a mocked vote of +1 or -1 on a perk
    private PerkVote voteOn(Perk perk, int value) {
        PerkVote vote = mock(PerkVote.class);
        when(vote.getPerk()).thenReturn(perk);
        when(vote.getValue()).thenReturn((short) value);
        return vote;
    }

    // With sort=votes the perk with the highest score comes first and the lowest score comes last.
    // Setup: perks with scores 0, +2 (two upvotes) and -1 (one downvote), returned in expiry order.
    // Expected order after sorting: +2, 0, -1. Also checks that the page remembers sort=votes.
    @Test
    void sortByVotesPutsHighestScoreFirst() {
        loginAs(null);
        Perk none = perkWithId(1L);
        Perk popular = perkWithId(2L);
        Perk disliked = perkWithId(3L);
        when(perkService.searchActivePerks("", "", null)).thenReturn(List.of(none, popular, disliked));
        List<PerkVote> votes = List.of(voteOn(popular, 1), voteOn(popular, 1), voteOn(disliked, -1));
        when(perkService.votesForPerks(anyList())).thenReturn(votes);

        controller.perks(null, null, false, "votes", request, model);

        List<PerkController.PerkRow> rows = rowsFromModel();
        assertSame(popular, rows.get(0).getPerk());
        assertEquals(2, rows.get(0).getVoteScore());
        assertSame(none, rows.get(1).getPerk());
        assertSame(disliked, rows.get(2).getPerk());
        assertEquals("votes", model.getAttribute("sort"));
    }

    // When two perks have the same score, sorting by votes must not shuffle them: they keep the
    // soonest-expiry order they came in with. Setup: only the third perk has a vote, so the first
    // two are tied at 0. Expected order: third, first, second.
    @Test
    void sortByVotesKeepsExpiryOrderForTies() {
        loginAs(null);
        Perk first = perkWithId(1L);
        Perk second = perkWithId(2L);
        Perk third = perkWithId(3L);
        when(perkService.searchActivePerks("", "", null)).thenReturn(List.of(first, second, third));
        List<PerkVote> votes = List.of(voteOn(third, 1));
        when(perkService.votesForPerks(anyList())).thenReturn(votes);

        controller.perks(null, null, false, "votes", request, model);

        List<PerkController.PerkRow> rows = rowsFromModel();
        assertSame(third, rows.get(0).getPerk());
        assertSame(first, rows.get(1).getPerk());
        assertSame(second, rows.get(2).getPerk());
    }

    // With sort=expiry the controller must leave the search order alone, even if a later perk has
    // more votes. Setup: the second perk has an upvote, but "soon" must still come first.
    @Test
    void sortByExpiryKeepsTheSearchOrderEvenWhenVotesDiffer() {
        loginAs(null);
        Perk soon = perkWithId(1L);
        Perk later = perkWithId(2L);
        when(perkService.searchActivePerks("", "", null)).thenReturn(List.of(soon, later));
        List<PerkVote> votes = List.of(voteOn(later, 1));
        when(perkService.votesForPerks(anyList())).thenReturn(votes);

        controller.perks(null, null, false, "expiry", request, model);

        List<PerkController.PerkRow> rows = rowsFromModel();
        assertSame(soon, rows.get(0).getPerk());
        assertSame(later, rows.get(1).getPerk());
        assertEquals("expiry", model.getAttribute("sort"));
    }

    // An unrecognised sort value (for example someone typing ?sort=banana in the URL) must not break
    // the page. The controller falls back to the default, sort by expiry.
    @Test
    void unknownSortFallsBackToExpiry() {
        loginAs(null);
        when(perkService.searchActivePerks(any(), any(), any())).thenReturn(List.of());

        controller.perks(null, null, false, "banana", request, model);

        assertEquals("expiry", model.getAttribute("sort"));
    }

    // The older method without a sort argument (used by existing tests) must still work and
    // default to sorting by expiry.
    @Test
    void perksWithoutASortChoiceDefaultsToExpiry() {
        loginAs(null);
        when(perkService.searchActivePerks(any(), any(), any())).thenReturn(List.of());

        controller.perks(null, null, false, request, model);

        assertEquals("expiry", model.getAttribute("sort"));
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
