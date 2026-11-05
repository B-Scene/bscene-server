package com.umc.bscene.global.security.oauth.apple;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppleCredentialCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String VALUE = "apple-sensitive-refresh-token";
    private static final String CONTEXT = "credential-subject-hash";

    @Test
    void encryptWithRandomNonceAndAuthenticateRecordContext() {
        var cipher = new AppleCredentialCipher(KEY);
        String first = cipher.encrypt(VALUE, CONTEXT);
        String second = cipher.encrypt(VALUE, CONTEXT);

        assertThat(first).startsWith("v1.").isNotEqualTo(second).doesNotContain(VALUE);
        assertThat(cipher.decrypt(first, CONTEXT)).isEqualTo(VALUE);
        assertThat(cipher.decrypt(second, CONTEXT)).isEqualTo(VALUE);
    }

    @Test
    void rejectChangedContextWrongKeyAndModifiedCiphertext() {
        var cipher = new AppleCredentialCipher(KEY);
        String encrypted = cipher.encrypt(VALUE, CONTEXT);
        String otherKey = Base64.getEncoder().encodeToString("a".repeat(32).getBytes());
        byte[] payload = Base64.getUrlDecoder().decode(encrypted.substring(3));
        payload[payload.length - 1] ^= 1;
        String tampered = "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);

        assertSafeFailure(() -> cipher.decrypt(encrypted, "another-record"));
        assertSafeFailure(() -> new AppleCredentialCipher(otherKey).decrypt(encrypted, CONTEXT));
        assertSafeFailure(() -> cipher.decrypt(tampered, CONTEXT));
    }

    @Test
    void keepMissingKeyLazyButRequire256BitConfiguration() {
        for (String key : List.of("", "not-base64", Base64.getEncoder().encodeToString(new byte[16]))) {
            var cipher = new AppleCredentialCipher(key);
            assertSafeFailure(cipher::validateConfiguration);
            assertSafeFailure(() -> cipher.encrypt(VALUE, CONTEXT));
        }
    }

    @Test
    void rejectMalformedOrUnsupportedStoredPayloadWithoutLeakingIt() {
        var cipher = new AppleCredentialCipher(KEY);
        for (String value : List.of("plain-token", "v2.token", "v1.%%%", "v1.YQ")) {
            assertSafeFailure(() -> cipher.decrypt(value, CONTEXT));
        }
    }

    private void assertSafeFailure(Runnable action) {
        IllegalStateException error = assertThrows(IllegalStateException.class, action::run);
        assertThat(error.getMessage()).doesNotContain(VALUE, KEY);
        assertThat(error.getCause()).isNull();
    }
}
