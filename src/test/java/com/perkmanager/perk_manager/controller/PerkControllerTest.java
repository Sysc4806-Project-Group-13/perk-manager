package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.services.UserService;
import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.services.MembershipService;
import jakarta.servlet.http.HttpServletRequest;
import com.perkmanager.perk_manager.search.PerkSearchCriteria;
import com.perkmanager.perk_manager.model.DiscountType;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.model.PerkVote;

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
import org.mockito.ArgumentCaptor;


// Tests the PerkController independently of the service implementation
//  in isolation: services are mocked, no Spring context is started
class PerkControllerTest {

    private PerkService perkService;
    private UserService userService;
    private MembershipService membershipService;
    private PerkController controller;
    private HttpServletRequest request;
    private Model model;
    private MockedStatic<AuthController> auth;

    @BeforeEach
    void setUp() {
        perkService = mock(PerkService.class);
        userService = mock(UserService.class);

        controller = new PerkController(
                perkService,
                userService
        );

        request = mock(HttpServletRequest.class);
        model = new ExtendedModelMap();

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

    // ---------- helpers for the /perks tests ----------

    // Calls the page with every filter unset except the ones passed in
    private String perksPage(String q, boolean mine) {
        return controller.perks(q, false, null, null, null, null, mine, request, model);
    }

    private PerkSearchCriteria searchedCriteria() {
        ArgumentCaptor<PerkSearchCriteria> captor = ArgumentCaptor.forClass(PerkSearchCriteria.class);
        verify(perkService).searchActivePerks(captor.capture());
        return captor.getValue();
    }

    // ---------- HTML page: GET /perks ----------

    @Test
    void perksReturnsPerksViewName() {
        loginAs(null);

        String view = perksPage(null, false);

        assertEquals("perks", view);
        assertEquals("perks", model.getAttribute("activeNav"));
    }

    @Test
    void perksTrimsInputsAndTreatsNullAsEmpty() {
        loginAs(null);

        perksPage("  coffee  ", false);

        PerkSearchCriteria c = searchedCriteria();
        assertEquals("coffee", c.keyword());
        assertEquals("", c.area());
        assertEquals("coffee", model.getAttribute("q"));
        assertEquals("", model.getAttribute("area"));
    }

    @Test
    void perksPassesEveryFilterToTheService() {
        loginAs(5L);
        Set<Long> myIds = Set.of(1L);
        when(userService.membershipIdsForUser(5L)).thenReturn(myIds);

        controller.perks("  tea ", true, "Ontario", 2L, "percentage", 30, true, request, model);

        PerkSearchCriteria c = searchedCriteria();
        assertEquals("tea", c.keyword());
        assertTrue(c.productOnly());
        assertEquals("Ontario", c.area());
        assertEquals(2L, c.membershipId());
        assertEquals(DiscountType.PERCENTAGE, c.discountType());
        assertEquals(30, c.expiringWithinDays());
        assertSame(myIds, c.onlyMembershipIds());
        assertEquals(true, model.getAttribute("filtersActive"));
    }

    @Test
    void perksIgnoresInvalidBenefitTypeExpiry() {
        loginAs(null);

        controller.perks(null, false, null, null, "bogus", -5, false, request, model);

        PerkSearchCriteria c = searchedCriteria();
        assertNull(c.discountType());
        assertNull(c.expiringWithinDays());
        assertEquals(false, model.getAttribute("filtersActive"));
    }

    @Test
    void perksForVisitorIsNotLoggedInAndNeverLooksUpMemberships() {
        loginAs(null);
        Perk perk = perkInMembership(1L);
        when(perkService.searchActivePerks(any(PerkSearchCriteria.class))).thenReturn(List.of(perk));

        // mine=true is ignored for visitors because onlyMine = mine && loggedIn
        perksPage(null, true);

        assertEquals(false, model.getAttribute("loggedIn"));
        assertEquals(false, model.getAttribute("mine"));
        verifyNoInteractions(userService);
        assertNull(searchedCriteria().onlyMembershipIds());
        assertFalse(rowsFromModel().get(0).isMatchesProfile());
    }

    @Test
    void perksForLoggedInUserFlagsMatchesButDoesNotFilterWhenMineIsFalse() {
        loginAs(5L);
        when(userService.membershipIdsForUser(5L)).thenReturn(Set.of(1L));
        Perk mine = perkInMembership(1L);
        Perk other = perkInMembership(2L);
        when(perkService.searchActivePerks(any(PerkSearchCriteria.class))).thenReturn(List.of(mine, other));

        perksPage(null, false);

        assertNull(searchedCriteria().onlyMembershipIds());   // null = don't filter by membership
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
        when(perkService.searchActivePerks(any(PerkSearchCriteria.class)))
                .thenReturn(List.of(mine));

        perksPage("tea", true);

        assertSame(myIds, searchedCriteria().onlyMembershipIds());
        assertEquals(true, model.getAttribute("mine"));
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



}
