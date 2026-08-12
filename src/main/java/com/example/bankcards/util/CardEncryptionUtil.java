package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CardEncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec secretKey;

    public CardEncryptionUtil(@Value("${app.security.card-encryption-secret}") String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init card encryption key", e);
        }
    }

    public String encrypt(String plainCardNumber) {
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainCardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Card number encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        try {
            String[] parts = stored.split(":");
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Card number decryption failed", e);
        }
    }
}
