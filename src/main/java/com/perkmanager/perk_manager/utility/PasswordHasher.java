package com.perkmanager.perk_manager.utility;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// Hashes and verifies passwords. New passwords use salted PBKDF2; the SHA-256 hashes
// written by the V4 dummy-user migration are still accepted so that account keeps working.
public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    // Produces "pbkdf2$<iterations>$<salt base64>$<hash base64>" (well under the 255-char column)
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = pbkdf2(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(derived);
    }

    public static boolean matches(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        if (isLegacySha256(stored)) {
            return constantTimeEquals(sha256Hex(password), stored.toLowerCase());
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // True for old unsalted SHA-256 hashes, which should be replaced after a successful login
    public static boolean needsUpgrade(String stored) {
        return stored != null && isLegacySha256(stored);
    }

    private static boolean isLegacySha256(String stored) {
        return stored.length() == 64 && stored.matches("[0-9a-fA-F]{64}");
    }

    private static String sha256Hex(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
