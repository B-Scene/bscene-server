package com.umc.bscene.global.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppleMobileOauthConfigTest {

    @Test
    void preserveStartupWithMissingAppleSecretsButRejectAuthentication() {
        var configuration = new AppleMobileOauthConfig();
        var verifier = assertDoesNotThrow(() -> configuration.appleIdTokenVerifier(""));
        var generator = assertDoesNotThrow(() -> configuration.appleClientSecretGenerator("", "", "", ""));
        var client = assertDoesNotThrow(() -> configuration.appleTokenClient("", generator));

        assertThrows(IllegalStateException.class, () -> verifier.verify("token", "nonce"));
        assertThrows(IllegalStateException.class, generator::generate);
        assertThrows(IllegalStateException.class, () -> client.exchange("code"));
    }
}
