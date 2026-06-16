package com.sushil.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class AesEncryptionUtil {

    private static final String ALGORITHM  = "AES/CBC/PKCS5Padding";
    private static final String DIGEST_ALG = "SHA-256";

    @Value("${app.encryption.passphrase}")
    private String passphrase;

    private SecretKeySpec  keySpec;
    private IvParameterSpec ivSpec;

    @PostConstruct
    void init() throws Exception {
        byte[] hash = MessageDigest.getInstance(DIGEST_ALG)
                .digest(passphrase.getBytes(StandardCharsets.UTF_8));
        keySpec = new SecretKeySpec(hash, "AES");
        ivSpec  = new IvParameterSpec(Arrays.copyOf(hash, 16));
        log.debug("[AES] Key and IV initialised from passphrase");
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            log.error("[AES] Encryption failed: {}", ex.getMessage());
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("[AES] Decryption failed: {}", ex.getMessage());
            throw new RuntimeException("Decryption failed", ex);
        }
    }
}
