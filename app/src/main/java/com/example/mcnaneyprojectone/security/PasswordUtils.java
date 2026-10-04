
package com.example.mcnaneyprojectone.security;

import android.util.Base64;
import android.util.Log;

import com.lambdapioneer.argon2kt.Argon2Kt;
import com.lambdapioneer.argon2kt.Argon2Mode;
import com.lambdapioneer.argon2kt.Argon2KtResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtils {

    private static final String TAG = "PasswordUtils";

    private static final String LEGACY_ALGORITHM =
            "PBKDF2WithHmacSHA256";

    private static final String LEGACY_FORMAT =
            "pbkdf2-sha256";

    private static final int LEGACY_ITERATIONS = 600000;
    private static final int LEGACY_KEY_LENGTH = 256;

    private static final int SALT_LENGTH = 16;

    // OWASP-recommended Argon2id configuration:
    // 19 MiB memory, 2 iterations, 1 parallel lane.
    private static final int ARGON_ITERATIONS = 2;
    private static final int ARGON_MEMORY_KIB = 19456;
    private static final int ARGON_PARALLELISM = 1;
    private static final int ARGON_HASH_LENGTH = 32;

    private PasswordUtils() {
    }

    // Hash new and changed passwords using Argon2id.
    public static String hashPassword(String password) {

        if (password == null) {
            throw new IllegalArgumentException(
                    "Password cannot be null"
            );
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] passwordBytes =
                password.getBytes(StandardCharsets.UTF_8);

        new SecureRandom().nextBytes(salt);

        long start = System.nanoTime();

        try {

            Argon2Kt argon2 = new Argon2Kt();

            Argon2KtResult result = argon2.hash(
                    Argon2Mode.ARGON2_ID,
                    passwordBytes,
                    salt,
                    ARGON_ITERATIONS,
                    ARGON_MEMORY_KIB,
                    ARGON_PARALLELISM,
                    ARGON_HASH_LENGTH
            );

            return result.encodedOutputAsString();

        } finally {

            Arrays.fill(passwordBytes, (byte) 0);
            Arrays.fill(salt, (byte) 0);

            long elapsed =
                    (System.nanoTime() - start) / 1000000L;

            Log.d(
                    TAG,
                    "Argon2id hashing took " + elapsed + " ms"
            );
        }
    }

    public static boolean verifyPassword(
            String password,
            String storedPassword) {

        if (password == null || storedPassword == null) {
            return false;
        }

        long start = System.nanoTime();

        try {

            // Current password format.
            if (storedPassword.startsWith("$argon2id$")) {

                byte[] passwordBytes =
                        password.getBytes(StandardCharsets.UTF_8);

                try {

                    Argon2Kt argon2 = new Argon2Kt();

                    return argon2.verify(
                            Argon2Mode.ARGON2_ID,
                            storedPassword,
                            passwordBytes
                    );

                } finally {
                    Arrays.fill(passwordBytes, (byte) 0);
                }
            }

            // Existing PBKDF2 password formats.
            return verifyLegacyPassword(
                    password,
                    storedPassword
            );

        } catch (IllegalArgumentException e) {
            return false;

        } finally {

            long elapsed =
                    (System.nanoTime() - start) / 1000000L;

            Log.d(
                    TAG,
                    "Password verification took " +
                            elapsed + " ms"
            );
        }
    }

    // Used after successful login to migrate
    // older password hashes to Argon2id.
    public static boolean needsUpgrade(
            String storedPassword) {

        return storedPassword != null &&
                !storedPassword.startsWith("$argon2id$");
    }

    private static boolean verifyLegacyPassword(
            String password,
            String storedPassword) {

        try {

            String[] parts =
                    storedPassword.split(":", -1);

            byte[] salt;
            byte[] storedHash;
            int iterations;

            if (parts.length == 4) {

                if (!LEGACY_FORMAT.equals(parts[0])) {
                    return false;
                }

                iterations = Integer.parseInt(parts[1]);

                if (iterations < 1 ||
                        iterations > 2000000) {
                    return false;
                }

                salt = Base64.decode(
                        parts[2],
                        Base64.NO_WRAP
                );

                storedHash = Base64.decode(
                        parts[3],
                        Base64.NO_WRAP
                );

            } else if (parts.length == 2) {

                iterations = LEGACY_ITERATIONS;

                salt = Base64.decode(
                        parts[0],
                        Base64.NO_WRAP
                );

                storedHash = Base64.decode(
                        parts[1],
                        Base64.NO_WRAP
                );

            } else {
                return false;
            }

            if (salt.length != SALT_LENGTH ||
                    storedHash.length != LEGACY_KEY_LENGTH / 8) {
                return false;
            }

            byte[] computedHash = generateLegacyHash(
                    password,
                    salt,
                    iterations
            );

            try {

                return MessageDigest.isEqual(
                        storedHash,
                        computedHash
                );

            } finally {
                Arrays.fill(computedHash, (byte) 0);
            }

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] generateLegacyHash(
            String password,
            byte[] salt,
            int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                LEGACY_KEY_LENGTH
        );

        try {

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            LEGACY_ALGORITHM
                    );

            return factory.generateSecret(spec)
                    .getEncoded();

        } catch (
                java.security.NoSuchAlgorithmException |
                InvalidKeySpecException e) {

            throw new IllegalStateException(
                    "Legacy password verification failed",
                    e
            );

        } finally {
            spec.clearPassword();
        }
    }
}
