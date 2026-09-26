package com.pds.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Encrypts every record file at rest with AES-256-GCM. The key is derived from
 * the app's master password via PBKDF2-HMAC-SHA256 (200,000 iterations) so the
 * plaintext password is never stored anywhere - only a salted verifier hash is,
 * for confirming the password on next launch. Every ciphertext carries its own
 * random salt + IV, so two identical records never produce identical bytes on
 * disk, and any tampering flips the GCM authentication tag (decrypt throws).
 */
public final class CryptoService {

    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 200_000;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    /** Derive a 256-bit AES key from a password + salt using PBKDF2-HMAC-SHA256. */
    private byte[] deriveKey(char[] password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    /** Encrypts plaintext bytes. Output layout: [salt(16)][iv(12)][ciphertext+tag]. */
    public byte[] encrypt(byte[] plaintext, char[] password) {
        try {
            byte[] salt = new byte[SALT_LEN];
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(salt);
            random.nextBytes(iv);
            byte[] key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext);
            Arrays.fill(key, (byte) 0);
            ByteBuffer buf = ByteBuffer.allocate(SALT_LEN + IV_LEN + ct.length);
            buf.put(salt).put(iv).put(ct);
            return buf.array();
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /** Decrypts data produced by {@link #encrypt}. Throws if the password is wrong or data is corrupted/tampered. */
    public byte[] decrypt(byte[] data, char[] password) {
        try {
            if (data.length < SALT_LEN + IV_LEN) throw new IllegalArgumentException("Data too short / corrupted");
            byte[] salt = Arrays.copyOfRange(data, 0, SALT_LEN);
            byte[] iv = Arrays.copyOfRange(data, SALT_LEN, SALT_LEN + IV_LEN);
            byte[] ct = Arrays.copyOfRange(data, SALT_LEN + IV_LEN, data.length);
            byte[] key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            Arrays.fill(key, (byte) 0);
            return pt;
        } catch (Exception e) {
            throw new SecurityException("Decryption failed: wrong password or corrupted/tampered file", e);
        }
    }

    /** Salted verifier used only to confirm the master password on startup - never stores the password itself. */
    public byte[] makeVerifier(char[] password, byte[] salt) {
        byte[] key = deriveKey(password, salt);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public byte[] newSalt() {
        byte[] s = new byte[SALT_LEN];
        random.nextBytes(s);
        return s;
    }

    public boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    public static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    public static String utf8(byte[] b) { return new String(b, StandardCharsets.UTF_8); }
}
