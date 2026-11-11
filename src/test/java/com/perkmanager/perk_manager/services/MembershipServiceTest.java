package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.repository.MembershipRepository;
import com.perkmanager.perk_manager.model.Membership;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Enables Mockito annotations for this test class
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    // Mock the repository so tests do not require a real database
    @Mock
    private MembershipRepository membershipRepository;

    // Automatically inject the mocked repository into the service
    @InjectMocks
    private MembershipService membershipService;

    // Test that all memberships are successfully retrieved
    @Test
    void getAllMembershipsReturnsMemberships() {

        // Create a sample membership object
        Membership membership = mock(Membership.class);

        // Configure the mock repository to return one membership
        when(membershipRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(membership));

        // Call the service method being tested
        List<Membership> result =
                membershipService.getAllMemberships();

        // Verify that the returned list contains one membership
        assertEquals(1, result.size());

        // Verify that the service called the repository method
        verify(membershipRepository)
                .findAllByOrderByNameAsc();
    }

    // Test that an empty list is returned when no memberships exist
    @Test
    void getAllMembershipsReturnsEmptyList() {

        // Configure the repository to return an empty list
        when(membershipRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of());

        // Retrieve memberships through the service
        List<Membership> result =
                membershipService.getAllMemberships();

        // Verify that the result contains no memberships
        assertTrue(result.isEmpty());

        // Verify that the repository was queried
        verify(membershipRepository)
                .findAllByOrderByNameAsc();
    }
}