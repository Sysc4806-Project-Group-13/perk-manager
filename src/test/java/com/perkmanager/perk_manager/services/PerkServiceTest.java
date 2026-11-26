package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.repository.MembershipRepository;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.repository.PerkVoteRepository;
import com.perkmanager.perk_manager.repository.UserRepository;
import com.perkmanager.perk_manager.model.PerkVote;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.model.DiscountType;
import com.perkmanager.perk_manager.search.PerkSearchCriteria;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Enables Mockito annotations for this test class
@ExtendWith(MockitoExtension.class)
class PerkServiceTest {

    // Mock the repositories to avoid accessing the actual database
    @Mock
    private PerkRepository perkRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PerkVoteRepository voteRepository;

    @Mock
    private UserRepository userRepository;

    // Inject the mocked repositories into PerkService
    @InjectMocks
    private PerkService perkService;

    // Test that perks belonging to an existing membership are returned
    @Test
    void getPerksByMembershipReturnsMatchingPerks() {

        // Define the membership ID being requested
        Long membershipId = 1L;

        // Create a sample perk object
        Perk perk = mock(Perk.class);

        // Simulate that the requested membership exists
        when(membershipRepository.existsById(membershipId))
                .thenReturn(true);

        // Simulate the repository returning one matching perk
        when(perkRepository.findByMembershipId(membershipId))
                .thenReturn(List.of(perk));

        // Call the service method
        List<Perk> result =
                perkService.getPerksByMembership(membershipId);

        // Verify that one perk was returned
        assertEquals(1, result.size());

        // Verify that the perk repository was queried correctly
        verify(perkRepository)
                .findByMembershipId(membershipId);
    }

    // Test that an invalid membership ID is rejected
    @Test
    void getPerksByMembershipRejectsInvalidId() {

        // Attempt to retrieve perks using a negative membership ID
        assertThrows(
                ResponseStatusException.class,
                () -> perkService.getPerksByMembership(-1L)
        );

        // Verify that neither repository is accessed
        // because the ID is invalid
        verifyNoInteractions(
                membershipRepository,
                perkRepository
        );
    }

    // Test that an error is returned when the membership does not exist
    @Test
    void getPerksByMembershipRejectsMissingMembership() {

        // Use an ID that represents a nonexistent membership
        Long membershipId = 99L;

        // Simulate that the membership does not exist
        when(membershipRepository.existsById(membershipId))
                .thenReturn(false);

        // Verify that the service throws an exception
        assertThrows(
                ResponseStatusException.class,
                () -> perkService.getPerksByMembership(membershipId)
        );

        // Verify that perks are not retrieved for a nonexistent membership
        verifyNoInteractions(perkRepository);
    }

    // Test that an empty list is returned when a membership has no perks
    @Test
    void getPerksByMembershipReturnsEmptyList() {

        // Define an existing membership ID
        Long membershipId = 1L;

        // Simulate that the membership exists
        when(membershipRepository.existsById(membershipId))
                .thenReturn(true);

        // Simulate that the membership has no associated perks
        when(perkRepository.findByMembershipId(membershipId))
                .thenReturn(List.of());

        // Retrieve perks through the service
        List<Perk> result =
                perkService.getPerksByMembership(membershipId);

        // Verify that the result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    void castVoteCreatesVoteWhenUserHasNotVoted() {
        User user = mock(User.class);
        Perk perk = mock(Perk.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(perkRepository.findById(12L)).thenReturn(Optional.of(perk));
        when(voteRepository.findByUserIdAndPerkId(7L, 12L)).thenReturn(Optional.empty());

        perkService.castVote(7L, 12L, (short) 1);

        verify(voteRepository).save(argThat(vote -> vote.getValue() == 1
                && vote.getUser() == user && vote.getPerk() == perk));
    }

    @Test
    void castVoteChangesExistingVoteToOppositeDirection() {
        User user = mock(User.class);
        Perk perk = mock(Perk.class);
        PerkVote existing = new PerkVote(user, perk, (short) 1);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(perkRepository.findById(12L)).thenReturn(Optional.of(perk));
        when(voteRepository.findByUserIdAndPerkId(7L, 12L)).thenReturn(Optional.of(existing));

        perkService.castVote(7L, 12L, (short) -1);

        assertEquals(-1, existing.getValue());
        verify(voteRepository, never()).save(any());
        verify(voteRepository, never()).delete(any(PerkVote.class));
    }

    @Test
    void castingSameDirectionAgainRemovesVote() {
        User user = mock(User.class);
        Perk perk = mock(Perk.class);
        PerkVote existing = new PerkVote(user, perk, (short) -1);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(perkRepository.findById(12L)).thenReturn(Optional.of(perk));
        when(voteRepository.findByUserIdAndPerkId(7L, 12L)).thenReturn(Optional.of(existing));

        perkService.castVote(7L, 12L, (short) -1);

        verify(voteRepository).delete(existing);
    }

    @Test
    void castVoteRejectsInvalidDirectionBeforeDatabaseAccess() {
        assertThrows(ResponseStatusException.class,
                () -> perkService.castVote(7L, 12L, (short) 0));
        verifyNoInteractions(userRepository, perkRepository, voteRepository);
    }

    @Test
    void removeVoteDeletesExistingVote() {
        PerkVote existing = new PerkVote(mock(User.class), mock(Perk.class), (short) 1);
        when(voteRepository.findByUserIdAndPerkId(7L, 12L)).thenReturn(Optional.of(existing));

        perkService.removeVote(7L, 12L);

        verify(voteRepository).delete(existing);
    }

    // ---------- searchActivePerks(criteria) ----------

    private Perk perk(Long membershipId, DiscountType type, String area, LocalDate expiry) {
        Membership membership = mock(Membership.class);
        lenient().when(membership.getId()).thenReturn(membershipId);
        Perk perk = new Perk("Title", "Product", type, membership);
        perk.setGeographicArea(area);
        perk.setExpiryDate(expiry);
        return perk;
    }

    @Test
    void searchUsesProductOnlyQueryWhenProductOnlyIsSet() {
        Perk movie = perk(1L, DiscountType.PERCENTAGE, null, null);
        when(perkRepository.searchActiveByProduct(eq("movie"), any(LocalDate.class))).thenReturn(List.of(movie));

        List<Perk> result = perkService.searchActivePerks(
                new PerkSearchCriteria("movie", true, null, null, null, null, null));

        assertEquals(List.of(movie), result);
        verify(perkRepository, never()).searchActive(any(), any());
    }

    @Test
    void searchUsesFullTextQueryByDefault() {
        Perk perk = perk(1L, DiscountType.PERCENTAGE, null, null);
        when(perkRepository.searchActive(eq("visa"), any(LocalDate.class))).thenReturn(List.of(perk));

        List<Perk> result = perkService.searchActivePerks(
                new PerkSearchCriteria("visa", false, null, null, null, null, null));

        assertEquals(List.of(perk), result);
        verify(perkRepository, never()).searchActiveByProduct(any(), any());
    }

    @Test
    void searchFiltersByMembership() {
        Perk visa = perk(1L, DiscountType.PERCENTAGE, null, null);
        Perk caa = perk(2L, DiscountType.PERCENTAGE, null, null);
        when(perkRepository.searchActive(eq(""), any(LocalDate.class))).thenReturn(List.of(visa, caa));

        List<Perk> result = perkService.searchActivePerks(
                new PerkSearchCriteria("", false, null, 1L, null, null, null));

        assertEquals(List.of(visa), result);
    }

    @Test
    void searchFiltersByDiscountType() {
        Perk percent = perk(1L, DiscountType.PERCENTAGE, null, null);
        Perk fixed = perk(1L, DiscountType.FIXED_AMOUNT, null, null);
        when(perkRepository.searchActive(eq(""), any(LocalDate.class))).thenReturn(List.of(percent, fixed));

        List<Perk> result = perkService.searchActivePerks(
                new PerkSearchCriteria("", false, null, null, DiscountType.FIXED_AMOUNT, null, null));

        assertEquals(List.of(fixed), result);
    }

    @Test
    void searchExpiringWithinKeepsPerksInsideTheWindowAndDropsNeverExpiring() {
        LocalDate today = LocalDate.now();
        Perk soon = perk(1L, DiscountType.PERCENTAGE, null, today.plusDays(10));
        Perk onTheEdge = perk(1L, DiscountType.PERCENTAGE, null, today.plusDays(30));
        Perk later = perk(1L, DiscountType.PERCENTAGE, null, today.plusDays(100));
        Perk never = perk(1L, DiscountType.PERCENTAGE, null, null);
        when(perkRepository.searchActive(eq(""), any(LocalDate.class)))
                .thenReturn(List.of(soon, onTheEdge, later, never));

        List<Perk> result = perkService.searchActivePerks(
                new PerkSearchCriteria("", false, null, null, null, 30, null));

        assertEquals(List.of(soon, onTheEdge), result);
    }

    @Test
    void searchAreaKeepsUnrestrictedPerksAndMatchesCaseInsensitively() {
        Perk anywhere = perk(1L, DiscountType.PERCENTAGE, null, null);
        Perk ontario = perk(1L, DiscountType.PERCENTAGE, "Ontario", null);
        Perk quebec = perk(1L, DiscountType.PERCENTAGE, "Quebec", null);
        when(perkRepository.searchActive(eq(""), any(LocalDate.class)))
                .thenReturn(List.of(anywhere, ontario, quebec));

        List<Perk> result = perkService.searchActivePerks(
                new PerkSearchCriteria("", false, "ONTARIO", null, null, null, null));

        assertEquals(List.of(anywhere, ontario), result);
    }

    @Test
    void searchCombinesFilters() {
        LocalDate today = LocalDate.now();
        Perk match = perk(1L, DiscountType.PERCENTAGE, "Ontario", today.plusDays(5));
        Perk wrongType = perk(1L, DiscountType.FIXED_AMOUNT, "Ontario", today.plusDays(5));
        Perk notMine = perk(2L, DiscountType.PERCENTAGE, "Ontario", today.plusDays(5));
        Perk tooLate = perk(1L, DiscountType.PERCENTAGE, "Ontario", today.plusDays(50));
        when(perkRepository.searchActive(eq("tea"), any(LocalDate.class)))
                .thenReturn(List.of(match, wrongType, notMine, tooLate));

        List<Perk> result = perkService.searchActivePerks(new PerkSearchCriteria(
                "tea", false, "Ontario", null, DiscountType.PERCENTAGE, 30, Set.of(1L)));

        assertEquals(List.of(match), result);
    }

    @Test
    void threeArgumentSearchStillWorks() {
        Perk mine = perk(1L, DiscountType.PERCENTAGE, null, null);
        Perk other = perk(2L, DiscountType.PERCENTAGE, null, null);
        when(perkRepository.searchActive(eq("x"), any(LocalDate.class))).thenReturn(List.of(mine, other));

        assertEquals(List.of(mine), perkService.searchActivePerks("x", null, Set.of(1L)));
    }

    @Test
    void filterOptionsCollectsMembershipsAreasAndProducts() {
        when(membershipRepository.findAllByOrderByNameAsc()).thenReturn(List.of(mock(Membership.class)));
        when(perkRepository.findActiveAreas(any(LocalDate.class))).thenReturn(List.of("Canada"));
        when(perkRepository.findActiveProductNames(any(LocalDate.class))).thenReturn(List.of("Movie Tickets"));

        var options = perkService.filterOptions();

        assertEquals(1, options.getMemberships().size());
        assertEquals(List.of("Canada"), options.getAreas());
        assertEquals(List.of("Movie Tickets"), options.getProductNames());
    }

}
