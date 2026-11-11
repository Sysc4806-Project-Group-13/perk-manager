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

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void getAllMembershipsReturnsMemberships() {

        Membership membership = mock(Membership.class);

        when(membershipRepository.findAll())
                .thenReturn(List.of(membership));

        List<Membership> result =
                membershipService.getAllMemberships();

        assertEquals(1, result.size());

        verify(membershipRepository).findAll();
    }

    @Test
    void getAllMembershipsReturnsEmptyList() {

        when(membershipRepository.findAll())
                .thenReturn(List.of());

        List<Membership> result =
                membershipService.getAllMemberships();

        assertTrue(result.isEmpty());
    }
}