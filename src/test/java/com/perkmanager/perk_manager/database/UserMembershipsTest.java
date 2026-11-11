
package com.perkmanager.perk_manager.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MembershipDatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Helper: Create a unique test user
    private long createTestUser() {

        // Generate random user
        String username = "test-" + UUID.randomUUID();

        // Add random user into db
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                username,
                "test-password-hash"
        );

        // Return the user we just added
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username
        );
    }

    // Helper: Get an existing membership
    private long getMembershipId(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM memberships WHERE name = ?",
                Long.class,
                name
        );
    }

    // Test 1: Verify a user can add a membership
    @Test
    @Transactional
    void userShouldBeAbleToAddMembership() {

        // Use helper functions to create user and membership
        long userId = createTestUser();
        long membershipId = getMembershipId("Air Miles");

        // Add membership to user in db
        jdbcTemplate.update(
                "INSERT INTO user_memberships (user_id, membership_id) VALUES (?, ?)",
                userId,
                membershipId
        );

        // Ensure that the user is tied to exactly 1 membership
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memberships " +
                        "WHERE user_id = ? AND membership_id = ?",
                Integer.class,
                userId,
                membershipId
        );

        assertEquals(1, count);
    }

    // Test 2: Verify membership perks are correctly associated
    @Test
    @Transactional
    void membershipShouldReturnCorrectPerks() {

        // Get air miles membership
        long membershipId = getMembershipId("Air Miles");

        // Get perks on air miles
        List<Map<String, Object>> perks = jdbcTemplate.queryForList(
                "SELECT id, title, description, product_name, " +
                        "discount_type, discount_value, geographic_area, " +
                        "expiry_date, membership_id " +
                        "FROM perks WHERE membership_id = ?",
                membershipId
        );

        // Verify that the membership has perks
        assertFalse(perks.isEmpty(),
                "Air Miles should have associated perks");

        // Verify that all perks are air miles perks
        for (Map<String, Object> perk : perks) {

            assertEquals(
                    membershipId,
                    ((Number) perk.get("membership_id")).longValue(),
                    "Perk should belong to the correct membership"
            );

            // Verify important perk information exists
            assertNotNull(perk.get("title"));
            assertNotNull(perk.get("product_name"));
            assertNotNull(perk.get("discount_type"));
            assertNotNull(perk.get("discount_value"));
        }
    }

    // Test 3: Verify duplicate memberships are rejected
    @Test
    @Transactional
    void userShouldNotBeAbleToAddDuplicateMemberships() {

        // Use helper functions to create user and membership
        long userId = createTestUser();
        long membershipId = getMembershipId("Air Miles");

        // Add membership for the first time
        jdbcTemplate.update(
                "INSERT INTO user_memberships (user_id, membership_id) VALUES (?, ?)",
                userId,
                membershipId
        );

        // Ensure we can't add the same membership again
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO user_memberships (user_id, membership_id) VALUES (?, ?)",
                        userId,
                        membershipId
                )
        );
    }

    // Test 4: Different users should be able to share a membership
    @Test
    @Transactional
    void differentUsersShouldBeAbleToHaveSameMembership() {

        // Use helper functions to create user and membership
        long user1 = createTestUser();
        long user2 = createTestUser();
        long membershipId = getMembershipId("Air Miles");

        // Give one user the membership
        jdbcTemplate.update(
                "INSERT INTO user_memberships (user_id, membership_id) VALUES (?, ?)",
                user1,
                membershipId
        );

        // Give the other user the same membership
        jdbcTemplate.update(
                "INSERT INTO user_memberships (user_id, membership_id) VALUES (?, ?)",
                user2,
                membershipId
        );

        // Ensure that at least both users have the membership
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memberships WHERE membership_id = ?",
                Integer.class,
                membershipId
        );

        assertTrue(count >= 2);
    }
}
