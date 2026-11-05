package com.umc.bscene.global.security.oauth.apple;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public final class AppleCredentialCipher {

    private static final String PREFIX = "v1.";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MAX_TOKEN_LENGTH = 32768;

    private final String encodedKey;
    private final SecureRandom random = new SecureRandom();

    public AppleCredentialCipher(String base64Key) {
        this.encodedKey = base64Key;
    }

    public void validateConfiguration() {
        readKey();
    }

    public String encrypt(String value, String context) {
        SecretKeySpec key = readKey();
        validateInput(value, context);
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            byte[] encrypted = createCipher(Cipher.ENCRYPT_MODE, key, iv, context)
                    .doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Apple 자격정보 암호화에 실패했습니다.");
        }
    }

    public String decrypt(String value, String context) {
        SecretKeySpec key = readKey();
        try {
            byte[] payload = decodePayload(value);
            if (context == null || context.isBlank()) {
                throw new IllegalArgumentException();
            }
            byte[] iv = java.util.Arrays.copyOf(payload, IV_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            return new String(createCipher(Cipher.DECRYPT_MODE, key, iv, context).doFinal(encrypted),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Apple 자격정보 복호화에 실패했습니다.");
        }
    }

    private byte[] decodePayload(String value) {
        if (value == null || !value.startsWith(PREFIX) || value.length() > 65536) {
            throw new IllegalArgumentException();
        }
        byte[] payload = Base64.getUrlDecoder().decode(value.substring(PREFIX.length()));
        if (payload.length <= IV_BYTES + TAG_BITS / 8) {
            throw new IllegalArgumentException();
        }
        return payload;
    }

    private Cipher createCipher(int mode, SecretKeySpec key, byte[] iv, String context)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        // 다른 행으로 암호문을 옮겨도 사용되지 않도록 저장 식별자를 인증한다.
        cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private SecretKeySpec readKey() {
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey.trim());
            if (key.length != 32) {
                throw new IllegalArgumentException();
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Apple 자격정보 암호화 키는 Base64 형식의 256비트 키여야 합니다.");
        }
    }

    private void validateInput(String value, String context) {
        if (value == null || value.isBlank() || value.length() > MAX_TOKEN_LENGTH
                || value.getBytes(StandardCharsets.UTF_8).length > MAX_TOKEN_LENGTH
                || context == null || context.isBlank()) {
            throw new IllegalArgumentException("Apple 자격정보 암호화 입력이 올바르지 않습니다.");
        }
    }
}
