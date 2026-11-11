package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.services.MembershipService;
import com.perkmanager.perk_manager.model.Membership;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Tests the MembershipController independently of the service implementation
class MembershipControllerTest {

    // Test that the controller successfully returns all memberships
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
    }
}