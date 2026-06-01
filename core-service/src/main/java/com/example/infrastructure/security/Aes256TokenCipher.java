package com.example.infrastructure.security;

import com.example.application.exception.ConnectorValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class Aes256TokenCipher {
    private static final String PREFIX = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @ConfigProperty(name = "core.connector-token-encryption-key", defaultValue = "dev-only-connector-token-encryption-key-change-me")
    String encryptionSecret;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new ConnectorValidationException("connector_token_required");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":"
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new ConnectorValidationException("connector_token_encryption_failed");
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            throw new ConnectorValidationException("connector_token_required");
        }
        if (!value.startsWith(PREFIX + ":")) {
            return value;
        }
        try {
            String[] parts = value.split(":", 3);
            if (parts.length != 3) {
                throw new ConnectorValidationException("connector_token_invalid_ciphertext");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (ConnectorValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ConnectorValidationException("connector_token_decryption_failed");
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX + ":");
    }

    private SecretKeySpec key() throws Exception {
        byte[] secret = encryptionSecret == null ? new byte[0] : encryptionSecret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
