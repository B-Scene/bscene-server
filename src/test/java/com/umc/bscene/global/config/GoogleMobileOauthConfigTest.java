package com.umc.bscene.global.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleMobileOauthConfigTest {

    @Test
    void rejectAuthenticationWithoutBreakingStartupForBlankConfiguration() {
        var configuration = new GoogleMobileOauthConfig();

        var verifier = assertDoesNotThrow(() -> configuration.googleIdTokenVerifier(" , , "));

        assertThrows(IllegalStateException.class, () -> verifier.verify("not-a-token"));
    }
}
