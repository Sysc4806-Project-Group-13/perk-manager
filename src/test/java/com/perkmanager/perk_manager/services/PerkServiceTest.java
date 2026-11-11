package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.repository.MembershipRepository;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.model.Perk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
}