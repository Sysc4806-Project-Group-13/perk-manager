package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.services.MembershipService;
import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.services.UserService;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

// Tests the MembershipController independently of the service implementation
// in isolation: services are mocked, no Spring context is started
class MembershipControllerTest {

    private MembershipService membershipService;
    private PerkService perkService;
    private UserService userService;
    private MembershipController controller;
    private HttpServletRequest request;
    private Model model;
    private MockedStatic<AuthController> auth;

    @BeforeEach
    void setUp() {
        membershipService = mock(MembershipService.class);
        perkService = mock(PerkService.class);
        userService = mock(UserService.class);
        controller = new MembershipController(membershipService, perkService, userService);
        request = mock(HttpServletRequest.class);
        model = new ExtendedModelMap();

        // currentUserId(...) is static, so it has to be mocked statically
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

    private Membership membership(Long id) {
        Membership m = mock(Membership.class);
        when(m.getId()).thenReturn(id);
        return m;
    }

    private Perk perkOf(Membership membership) {
        Perk p = mock(Perk.class);
        when(p.getMembership()).thenReturn(membership);
        return p;
    }

    @SuppressWarnings("unchecked")
    private List<MembershipController.MembershipPerks> rowsFromModel() {
        return (List<MembershipController.MembershipPerks>) model.getAttribute("rows");
    }

    // ---------- JSON API: GET /api/memberships ----------

    @Test
    void getAllMembershipsReturnsMemberships() {
        Membership membership = mock(Membership.class);
        when(membershipService.getAllMemberships()).thenReturn(List.of(membership));

        List<Membership> result = controller.getAllMemberships();

        assertEquals(1, result.size());
        verify(membershipService).getAllMemberships();
    }

    @Test
    void getAllMembershipsReturnsEmptyList() {
        when(membershipService.getAllMemberships()).thenReturn(List.of());

        List<Membership> result = controller.getAllMemberships();

        assertTrue(result.isEmpty());
    }

    // ---------- HTML page: GET /memberships ----------

    @Test
    void membershipsReturnsMembershipsViewName() {
        loginAs(null);
        when(membershipService.getAllMemberships()).thenReturn(List.of());
        when(perkService.searchActivePerks("", null, null)).thenReturn(List.of());

        String view = controller.memberships(null, false, request, model);

        assertEquals("memberships", view);
        assertEquals("memberships", model.getAttribute("activeNav"));
    }

    @Test
    void membershipsTrimsKeywordAndTreatsNullAsEmpty() {
        loginAs(null);
        when(membershipService.getAllMemberships()).thenReturn(List.of());
        when(perkService.searchActivePerks("coffee", null, null)).thenReturn(List.of());

        // Whitespace is trimmed, and the service is called with null area and null membership filter
        controller.memberships("  coffee  ", false, request, model);

        verify(perkService).searchActivePerks(eq("coffee"), isNull(), isNull());
        assertEquals("coffee", model.getAttribute("q"));
    }

    @Test
    void membershipsForVisitorShowsAllAndNeverLooksUpMemberships() {
        loginAs(null);
        Membership a = membership(1L);
        Membership b = membership(2L);
        when(membershipService.getAllMemberships()).thenReturn(List.of(a, b));
        when(perkService.searchActivePerks("", null, null)).thenReturn(List.of());

        // mine=true is ignored for visitors because onlyMine = mine && loggedIn
        controller.memberships(null, true, request, model);

        assertEquals(false, model.getAttribute("loggedIn"));
        assertEquals(false, model.getAttribute("mine"));
        verifyNoInteractions(userService);

        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(2, rows.size());
        assertFalse(rows.get(0).isMine());
        assertFalse(rows.get(1).isMine());
    }

    @Test
    void membershipsForLoggedInUserFlagsOwnedButShowsAllWhenMineIsFalse() {
        loginAs(5L);
        when(userService.membershipIdsForUser(5L)).thenReturn(Set.of(1L));
        Membership owned = membership(1L);
        Membership other = membership(2L);
        when(membershipService.getAllMemberships()).thenReturn(List.of(owned, other));
        when(perkService.searchActivePerks("", null, null)).thenReturn(List.of());

        controller.memberships(null, false, request, model);

        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(2, rows.size());
        assertSame(owned, rows.get(0).getMembership());
        assertTrue(rows.get(0).isMine());
        assertSame(other, rows.get(1).getMembership());
        assertFalse(rows.get(1).isMine());
        assertEquals(true, model.getAttribute("loggedIn"));
        assertEquals(false, model.getAttribute("mine"));
    }

    @Test
    void membershipsWithMineKeepsOnlyUsersMemberships() {
        loginAs(5L);
        when(userService.membershipIdsForUser(5L)).thenReturn(Set.of(1L, 3L));
        Membership owned1 = membership(1L);
        Membership other = membership(2L);
        Membership owned3 = membership(3L);
        when(membershipService.getAllMemberships()).thenReturn(List.of(owned1, other, owned3));
        when(perkService.searchActivePerks("", null, null)).thenReturn(List.of());

        controller.memberships(null, true, request, model);

        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(2, rows.size());
        assertSame(owned1, rows.get(0).getMembership());
        assertSame(owned3, rows.get(1).getMembership());
        assertTrue(rows.get(0).isMine());
        assertTrue(rows.get(1).isMine());
        assertEquals(true, model.getAttribute("mine"));
    }

    @Test
    void membershipsGroupsPerksUnderTheirMembership() {
        loginAs(null);
        Membership a = membership(1L);
        Membership b = membership(2L);
        Perk perkA1 = perkOf(a);
        Perk perkA2 = perkOf(a);
        Perk perkB = perkOf(b);
        when(membershipService.getAllMemberships()).thenReturn(List.of(a, b));
        when(perkService.searchActivePerks("", null, null))
                .thenReturn(List.of(perkA1, perkB, perkA2));

        controller.memberships(null, false, request, model);

        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(List.of(perkA1, perkA2), rows.get(0).getPerks());
        assertEquals(List.of(perkB), rows.get(1).getPerks());
    }

    @Test
    void membershipsWithoutSearchKeepsMembershipsThatHaveNoPerks() {
        loginAs(null);
        Membership noPerks = membership(1L);
        when(membershipService.getAllMemberships()).thenReturn(List.of(noPerks));
        when(perkService.searchActivePerks("", null, null)).thenReturn(List.of());

        controller.memberships(null, false, request, model);

        // Not searching, so the empty-perk filter is skipped
        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).getPerks().isEmpty());
    }

    @Test
    void membershipsWithSearchDropsMembershipsWithNoMatchingPerks() {
        loginAs(null);
        Membership withPerk = membership(1L);
        Membership withoutPerk = membership(2L);
        Perk perk = perkOf(withPerk);
        when(membershipService.getAllMemberships()).thenReturn(List.of(withPerk, withoutPerk));
        when(perkService.searchActivePerks("coffee", null, null)).thenReturn(List.of(perk));

        controller.memberships("coffee", false, request, model);

        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(1, rows.size());
        assertSame(withPerk, rows.get(0).getMembership());
    }

    @Test
    void membershipsWithSearchAndMineCombinesBothFilters() {
        loginAs(5L);
        when(userService.membershipIdsForUser(5L)).thenReturn(Set.of(1L, 2L));
        Membership ownedWithPerk = membership(1L);
        Membership ownedNoPerk = membership(2L);
        Membership notOwnedWithPerk = membership(3L);
        when(membershipService.getAllMemberships())
                .thenReturn(List.of(ownedWithPerk, ownedNoPerk, notOwnedWithPerk));
        when(perkService.searchActivePerks("tea", null, null))
                .thenReturn(List.of(perkOf(ownedWithPerk), perkOf(notOwnedWithPerk)));

        controller.memberships("tea", true, request, model);

        // ownedNoPerk fails the search filter; notOwnedWithPerk fails the mine filter
        List<MembershipController.MembershipPerks> rows = rowsFromModel();
        assertEquals(1, rows.size());
        assertSame(ownedWithPerk, rows.get(0).getMembership());
        assertTrue(rows.get(0).isMine());
    }


    /** Test that the controller successfully returns all memberships
    @Test
    void getAllMembershipsReturnsMemberships() {

        // Create a mock service to simulate membership retrieval
        MembershipService service = mock(MembershipService.class);

        // Create the controller using the mock service
        MembershipController controller =
                new MembershipController(service);

        // Create a sample membership
        Membership membership = mock(Membership.class);

        // Configure the service to return one membership
        when(service.getAllMemberships())
                .thenReturn(List.of(membership));

        // Call the controller method
        List<Membership> result =
                controller.getAllMemberships();

        // Verify that the controller returns one membership
        assertEquals(1, result.size());

        // Verify that the controller calls the service
        verify(service).getAllMemberships();
    }

    // Test that the controller handles an empty membership list
    @Test
    void getAllMembershipsReturnsEmptyList() {

        // Create a mock service
        MembershipService service = mock(MembershipService.class);

        // Create the controller
        MembershipController controller =
                new MembershipController(service);

        // Configure the service to return no memberships
        when(service.getAllMemberships())
                .thenReturn(List.of());

        // Call the controller method
        List<Membership> result =
                controller.getAllMemberships();

        // Verify that an empty list is returned
        assertTrue(result.isEmpty());
    } **/
}