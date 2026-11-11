
package com.perkmanager.perk_manager.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InitialDatabaseTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Test 1: Verify that Flyway migrations have run successfully
    @Test
    void flywayMigrationsShouldBeSuccessful() {

        // Validate using Flyway's built in validation
        flyway.validate();

        // Verify that migrations have been applied
        assertTrue(flyway.info().applied().length >= 2);

        // Verify that the latest migration is version 4 (this needs to be updated each time a new migration file is added)
        assertEquals(
                "4",
                flyway.info().current().getVersion().getVersion()
        );
    }

    // Test 2: Verify that duplicate membership names are rejected
    @Test
    @Transactional
    void duplicateMembershipNamesShouldBeRejected() {

        // Create random membership name
        String membershipName = "Test-" + UUID.randomUUID();

        // Insert the first membership
        jdbcTemplate.update(
                "INSERT INTO memberships (name, description) VALUES (?, ?)",
                membershipName,
                "Test membership"
        );

        // Ensure that we cant insert another membership under the same name
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO memberships (name, description) VALUES (?, ?)",
                        membershipName,
                        "Duplicate membership"
                )
        );
    }
}

