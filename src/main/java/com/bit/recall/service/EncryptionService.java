package com.bit.recall.service;

import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** ====================================================================================
 * ENCRYPTION & DECRYPTION ENGINE (AES-256 GCM MODE) EXPLANATION
 * ====================================================================================
 *
 * 1. CORE CONCEPTS & TERMINOLOGY
 * ------------------------------------------------------------------------------------
 * - Plaintext         : The original, readable data (e.g., User's raw API key "sk-proj-1234").
 * - Ciphertext        : The scrambled, unreadable data stored in the database.
 * - Cipher            : The core cryptographic engine/algorithm that performs the
 *                       encryption and decryption math (e.g., AES).
 * - Secret Key        : A secret 256-bit (32-byte) key owned ONLY by our application server.
 *                       Without this master key, ciphertext cannot be decrypted back.
 * - IV (Init Vector)  : A 12-byte unique random salt generated dynamically for EVERY single
 *                       encryption operation.
 *                       Why is IV needed? If two users have the exact same API key, using a
 *                       fresh random IV guarantees that their stored Ciphertexts look
 *                       completely DIFFERENT in the database, preventing pattern attacks.
 * - Auth Tag (GCM)    : A 128-bit digital signature created automatically during encryption.
 *                       It guarantees "Data Integrity"—if someone manually tampers with
 *                       the ciphertext in the database, decryption will fail immediately.
 * - Base64 Encoding   : Raw encrypted output consists of non-printable binary bytes.
 *                       Base64 simply converts binary bytes into a clean, transportable
 *                       String format so it can be safely saved in DB columns or JSON APIs.
 *
 * ====================================================================================
 * 2. STEP-BY-STEP FLOW: ENCRYPTION PROCESS
 * ====================================================================================
 * [Step A] Input: Raw API Key String ("sk-proj-1234") + Master Secret Key (from config).
 * [Step B] Generate IV: Create a brand new, random 12-byte array using SecureRandom.
 * [Step C] Init Cipher: Configure Cipher with AES/GCM/NoPadding in ENCRYPT_MODE using
 *          the Secret Key and the generated IV.
 * [Step D] Encrypt Data: Execute cipher.doFinal() -> Converts Plaintext bytes into
 *          Encrypted Ciphertext bytes + 128-bit Auth Tag.
 * [Step E] Combine Arrays: Append IV + Ciphertext together into one array:
 *          Combined Byte Array = [ 12 Bytes IV | Remaining Bytes: Ciphertext + Auth Tag ]
 *          (Note: The IV is NOT secret, so storing it alongside the ciphertext is standard practice).
 * [Step F] Encode & Return: Convert the combined array to a Base64 String to store in DB.
 *
 * ====================================================================================
 * 3. STEP-BY-STEP FLOW: DECRYPTION PROCESS
 * ====================================================================================
 * [Step A] Input: Base64 String from DB + Master Secret Key (from config).
 * [Step B] Decode Base64: Convert the stored Base64 String back into raw Combined bytes.
 * [Step C] Extract IV: Read the FIRST 12 bytes from the array -> This is our original IV.
 * [Step D] Extract Ciphertext: Read the REMAINING bytes -> This is our Ciphertext + Auth Tag.
 * [Step E] Init Cipher: Configure Cipher with AES/GCM/NoPadding in DECRYPT_MODE using
 *          the Secret Key and extracted IV.
 * [Step F] Decrypt Data: Execute cipher.doFinal() -> Verifies Auth Tag integrity and
 *          reverses the math, returning original Plaintext bytes.
 * [Step G] Return Result: Convert bytes back to UTF-8 String ("sk-proj-1234").
 * ====================================================================================
 **/
@Singleton
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SecretKeySpec secretKey;

    public EncryptionService(@Property(name = "bit-recall.base64.secret") String base64SecretKey) {
        byte[] decodedKey = java.util.Base64.getDecoder().decode(base64SecretKey);
        this.secretKey = new javax.crypto.spec.SecretKeySpec(decodedKey, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

              Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

           // Store IV + CipherText together
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

           return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BitRecallException(BitRecallErrorCode.SERVER_ERROR, "Failed to encrypt data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);

            // Extract IV and CipherText
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BitRecallException(BitRecallErrorCode.SERVER_ERROR, "Failed to decrypt data", e);
        }
    }
}
