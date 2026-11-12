package com.perkmanager.perk_manager.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHasherTest {

    // SHA-256 of "password": the exact value V4__add_dummy_user.sql stores
    private static final String LEGACY_HASH =
            "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

    @Test
    void newHashVerifiesCorrectPasswordOnly() {
        String hash = PasswordHasher.hash("s3cretPass");

        assertTrue(PasswordHasher.matches("s3cretPass", hash));
        assertFalse(PasswordHasher.matches("s3cretPasS", hash));
    }

    @Test
    void samePasswordGetsDifferentHashesBecauseOfTheSalt() {
        assertNotEquals(PasswordHasher.hash("s3cretPass"), PasswordHasher.hash("s3cretPass"));
    }

    @Test
    void hashDoesNotContainThePlainPasswordAndFitsTheColumn() {
        String hash = PasswordHasher.hash("s3cretPass");

        assertFalse(hash.contains("s3cretPass"));
        assertTrue(hash.length() <= 255, "password_hash is VARCHAR(255)");
    }

    @Test
    void legacySha256HashFromTheDummyUserStillWorksAndNeedsUpgrade() {
        assertTrue(PasswordHasher.matches("password", LEGACY_HASH));
        assertFalse(PasswordHasher.matches("Password", LEGACY_HASH));
        assertTrue(PasswordHasher.needsUpgrade(LEGACY_HASH));
        assertFalse(PasswordHasher.needsUpgrade(PasswordHasher.hash("s3cretPass")));
    }

    @Test
    void garbageStoredValuesNeverMatch() {
        assertFalse(PasswordHasher.matches("x", "garbage"));
        assertFalse(PasswordHasher.matches("x", "pbkdf2$1$!!$!!"));
        assertFalse(PasswordHasher.matches("x", null));
        assertFalse(PasswordHasher.matches(null, LEGACY_HASH));
    }

}
