package com.perkmanager.perk_manager.controllers;

import com.perkmanager.perk_manager.services.PerkService;
import com.perkmanager.perk_manager.model.Perk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerkControllerTest {

    @Test
    void getPerksByMembershipReturnsPerks() {

        PerkService service = mock(PerkService.class);

        PerkController controller =
                new PerkController(service);

        Long membershipId = 1L;

        Perk perk = mock(Perk.class);

        when(service.getPerksByMembership(membershipId))
                .thenReturn(List.of(perk));

        List<Perk> result =
                controller.getPerksByMembership(membershipId);

        assertEquals(1, result.size());

        verify(service).getPerksByMembership(membershipId);
    }

    @Test
    void getPerksByMembershipReturnsEmptyList() {

        PerkService service = mock(PerkService.class);

        PerkController controller =
                new PerkController(service);

        when(service.getPerksByMembership(1L))
                .thenReturn(List.of());

        List<Perk> result =
                controller.getPerksByMembership(1L);

        assertTrue(result.isEmpty());
    }
}