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

@ExtendWith(MockitoExtension.class)
class PerkServiceTest {

    @Mock
    private PerkRepository perkRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private PerkService perkService;

    @Test
    void getPerksByMembershipReturnsMatchingPerks() {

        Long membershipId = 1L;

        Perk perk = mock(Perk.class);

        when(membershipRepository.existsById(membershipId))
                .thenReturn(true);

        when(perkRepository.findByMembershipId(
                membershipId))
                .thenReturn(List.of(perk));

        List<Perk> result =
                perkService.getPerksByMembership(membershipId);

        assertEquals(1, result.size());

        verify(perkRepository)
                .findByMembershipId(membershipId);
    }

    @Test
    void getPerksByMembershipRejectsInvalidId() {

        assertThrows(
                ResponseStatusException.class,
                () -> perkService.getPerksByMembership(-1L)
        );

        verifyNoInteractions(perkRepository);
    }

    @Test
    void getPerksByMembershipRejectsMissingMembership() {

        Long membershipId = 99L;

        when(membershipRepository.existsById(membershipId))
                .thenReturn(false);

        assertThrows(
                ResponseStatusException.class,
                () -> perkService.getPerksByMembership(membershipId)
        );

        verify(perkRepository, never())
                .findByMembershipId(anyLong());
    }

    @Test
    void getPerksByMembershipReturnsEmptyList() {

        Long membershipId = 1L;

        when(membershipRepository.existsById(membershipId))
                .thenReturn(true);

        when(perkRepository.findByMembershipId(
                membershipId))
                .thenReturn(List.of());

        List<Perk> result =
                perkService.getPerksByMembership(membershipId);

        assertTrue(result.isEmpty());
    }
}