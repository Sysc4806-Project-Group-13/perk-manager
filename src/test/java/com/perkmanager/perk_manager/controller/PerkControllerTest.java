package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.model.Perk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Tests the PerkController independently of the service implementation
class PerkControllerTest {

    // Test that the controller returns perks for a selected membership
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
    }
}