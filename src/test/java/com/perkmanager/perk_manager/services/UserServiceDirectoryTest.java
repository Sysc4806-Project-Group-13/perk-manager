package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.repository.MembershipRepository;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests UserService.listUserSummaries with mocked repositories, no database needed
@ExtendWith(MockitoExtension.class)
class UserServiceDirectoryTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PerkRepository perkRepository;

    @InjectMocks
    private UserService userService;

    // Helper: a mocked user with the given number of memberships
    private User user(Long id, String username, int memberships) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        Set<Membership> set = new java.util.HashSet<>();
        for (int i = 0; i < memberships; i++) {
            set.add(mock(Membership.class));
        }
        when(user.getMemberships()).thenReturn(set);
        return user;
    }

    // Each summary carries the id, username and number of memberships, in the order the repository gave.
    @Test
    void listUserSummariesReturnsUsernameAndMembershipCount() {
        // build the users first, then stub the repository (stubbing inside thenReturn breaks Mockito)
        User alex = user(1L, "alex", 2);
        User sam = user(2L, "sam", 0);
        List<User> users = List.of(alex, sam);
        when(userRepository.findAll(any(Sort.class))).thenReturn(users);

        List<UserSummary> result = userService.listUserSummaries();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("alex", result.get(0).getUsername());
        assertEquals(2, result.get(0).getMembershipCount());
        assertEquals("sam", result.get(1).getUsername());
        assertEquals(0, result.get(1).getMembershipCount());
    }

    // With no users the result is an empty list, not an error.
    @Test
    void listUserSummariesIsEmptyWhenThereAreNoUsers() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertTrue(userService.listUserSummaries().isEmpty());
    }
}