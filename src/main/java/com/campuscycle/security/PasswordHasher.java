package com.campuscycle.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Cryptographic Password Security Service.
 * Implements SHA-256 with unique cryptographic salting.
 */
public class PasswordHasher {
    private static final Logger LOGGER = Logger.getLogger(PasswordHasher.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Convenience record returned by the single-arg hashPassword(). */
    public record HashedCredentials(String hash, String salt) {}

    /**
     * Generates a cryptographically secure random salt (16 bytes Base64).
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Convenience: generate salt and hash in one call, returning both.
     */
    public static HashedCredentials hashPassword(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return new HashedCredentials(hash, salt);
    }

    /**
     * Hashes password with provided salt using SHA-256.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256 algorithm unavailable", e);
            throw new RuntimeException("Cryptographic error", e);
        }
    }

    /**
     * Verifies plain text password against stored hash and salt.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash, String storedSalt) {
        if (plainPassword == null || storedHash == null || storedSalt == null) {
            return false;
        }
        String calculatedHash = hashPassword(plainPassword, storedSalt);
        return MessageDigest.isEqual(
            calculatedHash.getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
