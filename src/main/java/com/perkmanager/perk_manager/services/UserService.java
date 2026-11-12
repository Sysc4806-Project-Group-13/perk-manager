package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.model.Membership;
import com.perkmanager.perk_manager.model.Perk;
import com.perkmanager.perk_manager.model.User;
import com.perkmanager.perk_manager.repository.MembershipRepository;
import com.perkmanager.perk_manager.repository.PerkRepository;
import com.perkmanager.perk_manager.repository.UserRepository;
import com.perkmanager.perk_manager.utility.PasswordHasher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Accounts, login checks and profile memberships
@Service
public class UserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,50}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PerkRepository perkRepository;

    public UserService(UserRepository userRepository, MembershipRepository membershipRepository,
                       PerkRepository perkRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.perkRepository = perkRepository;
    }

    // ---------- Accounts ----------

    // Creates an account. Throws IllegalArgumentException with a user-friendly message on bad input.
    @Transactional
    public User register(String username, String password, String confirmPassword) {
        String name = normalize(username);
        if (name == null || !USERNAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-50 characters: letters, numbers, dot, dash or underscore.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at most " + MAX_PASSWORD_LENGTH + " characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (userRepository.existsByUsername(name)) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        try {
            return userRepository.saveAndFlush(new User(name, PasswordHasher.hash(password)));
        } catch (DataIntegrityViolationException e) {
            // Two people registered the same name at the same moment; the database UNIQUE rule caught it
            throw new IllegalArgumentException("That username is already taken.");
        }
    }

    // Returns the user if the credentials are right, otherwise empty (never says which part was wrong)
    @Transactional
    public Optional<User> authenticate(String username, String password) {
        String name = normalize(username);
        if (name == null || password == null) {
            return Optional.empty();
        }
        Optional<User> found = userRepository.findByUsername(name);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        // Replace an old unsalted hash with a salted one now that we know the real password
        if (PasswordHasher.needsUpgrade(user.getPasswordHash())) {
            user.setPasswordHash(PasswordHasher.hash(password));
        }
        return Optional.of(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    // ---------- Profile memberships ----------

    // The memberships in the user's profile, alphabetical
    @Transactional(readOnly = true)
    public List<Membership> membershipsOf(Long userId) {
        return requireUser(userId).getMemberships().stream()
                .sorted(Comparator.comparing(Membership::getName))
                .toList();
    }

    // Ids of the memberships in a user's profile (empty if the user no longer exists)
    @Transactional(readOnly = true)
    public Set<Long> membershipIdsForUser(Long userId) {
        return userRepository.findById(userId)
                .map(this::membershipIdsOf)
                .orElse(Set.of());
    }

    // Memberships the user could still add (for the "add" dropdown)
    @Transactional(readOnly = true)
    public List<Membership> membershipsNotOwnedBy(Long userId) {
        Set<Long> owned = membershipIdsOf(requireUser(userId));
        return membershipRepository.findAllByOrderByNameAsc().stream()
                .filter(m -> !owned.contains(m.getId()))
                .toList();
    }

    @Transactional
    public void addMembership(Long userId, Long membershipId) {
        User user = requireUser(userId);
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("That membership does not exist."));
        user.getMemberships().add(membership); // a Set: adding twice changes nothing
    }

    @Transactional
    public void removeMembership(Long userId, Long membershipId) {
        requireUser(userId).getMemberships().removeIf(m -> m.getId().equals(membershipId));
    }

    // Active perks that belong to the user's memberships (soonest expiry first)
    @Transactional(readOnly = true)
    public List<Perk> perksForUser(Long userId) {
        Set<Long> ids = membershipIdsOf(requireUser(userId));
        if (ids.isEmpty()) {
            return List.of(); // the repository query must not be given an empty collection
        }
        return perkRepository.searchActiveForMemberships("", LocalDate.now(), ids);
    }

    // ---------- Helpers ----------

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user."));
    }

    private Set<Long> membershipIdsOf(User user) {
        return user.getMemberships().stream().map(Membership::getId).collect(Collectors.toSet());
    }

    // Trim and lowercase so "Alice " and "alice" are the same account; blank becomes null
    private String normalize(String username) {
        if (username == null) return null;
        String trimmed = username.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

}
