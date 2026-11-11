package com.perkmanager.perk_manager.controller;

import com.perkmanager.perk_manager.services.MembershipService;
import com.perkmanager.perk_manager.model.Membership;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipControllerTest {

    @Test
    void getAllMembershipsReturnsMemberships() {

        MembershipService service =
                mock(MembershipService.class);

        MembershipController controller =
                new MembershipController(service);

        Membership membership = mock(Membership.class);

        when(service.getAllMemberships())
                .thenReturn(List.of(membership));

        List<Membership> result =
                controller.getAllMemberships();

        assertEquals(1, result.size());

        verify(service).getAllMemberships();
    }

    @Test
    void getAllMembershipsReturnsEmptyList() {

        MembershipService service =
                mock(MembershipService.class);

        MembershipController controller =
                new MembershipController(service);

        when(service.getAllMemberships())
                .thenReturn(List.of());

        List<Membership> result =
                controller.getAllMemberships();

        assertTrue(result.isEmpty());
    }
}