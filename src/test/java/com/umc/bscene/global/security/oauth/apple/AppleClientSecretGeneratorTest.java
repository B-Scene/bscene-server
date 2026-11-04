package com.umc.bscene.global.security.oauth.apple;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleClientSecretGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-11-05T01:00:00Z");
    private static final String CLIENT_ID = "com.bscene.mobile";
    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY1234567";

    @TempDir
    Path temporaryDirectory;

    @Test
    void signsFiveMinuteClientSecretWithAppleClaims() throws Exception {
        KeyPair keyPair = createKeyPair("secp256r1");
        AppleClientSecretGenerator generator = createGenerator(writeKey(keyPair));

        SignedJWT token = SignedJWT.parse(generator.generate());

        assertEquals(JWSAlgorithm.ES256, token.getHeader().getAlgorithm());
        assertEquals(KEY_ID, token.getHeader().getKeyID());
        assertTrue(token.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic())));
        var claims = token.getJWTClaimsSet();
        assertEquals(TEAM_ID, claims.getIssuer());
        assertEquals(CLIENT_ID, claims.getSubject());
        assertEquals(List.of("https://appleid.apple.com"), claims.getAudience());
        assertEquals(NOW, claims.getIssueTime().toInstant());
        assertEquals(NOW.plusSeconds(300), claims.getExpirationTime().toInstant());
    }

    @Test
    void keepsMissingConfigurationLazyUntilAppleRequest() {
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator("", "", "", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class, generator::generate);

        assertNull(exception.getCause());
        assertFalse(exception.getMessage().contains(temporaryDirectory.toString()));
    }

    @Test
    void rejectsMissingOrMalformedDeveloperIdentifiers() {
        List<AppleClientSecretGenerator> invalidGenerators = List.of(
                new AppleClientSecretGenerator(" ", TEAM_ID, KEY_ID, "unused"),
                new AppleClientSecretGenerator(CLIENT_ID, "invalid", KEY_ID, "unused"),
                new AppleClientSecretGenerator(CLIENT_ID, TEAM_ID, "invalid", "unused"),
                new AppleClientSecretGenerator(CLIENT_ID, TEAM_ID, KEY_ID, "")
        );

        for (AppleClientSecretGenerator generator : invalidGenerators) {
            assertThrows(IllegalStateException.class, generator::generate);
        }
    }

    @Test
    void rejectsUnreadableAndMalformedKeysWithoutExposingPathOrContent() throws Exception {
        Path missing = temporaryDirectory.resolve("missing-private-key.p8");
        assertSanitizedFailure(createGenerator(missing));
        Path malformed = temporaryDirectory.resolve("malformed-private-key.p8");
        Files.writeString(malformed, "secret-private-key-content");

        IllegalStateException exception = assertSanitizedFailure(createGenerator(malformed));

        assertFalse(exception.getMessage().contains("secret-private-key-content"));
    }

    @Test
    void rejectsDifferentEllipticCurve() throws Exception {
        assertSanitizedFailure(createGenerator(writeKey(createKeyPair("secp384r1"))));
    }

    @Test
    void rejectsOversizedPrivateKeyFiles() throws Exception {
        Path oversized = temporaryDirectory.resolve("oversized-private-key.p8");
        Files.writeString(oversized, "x".repeat(16_385));

        assertSanitizedFailure(createGenerator(oversized));
    }

    private AppleClientSecretGenerator createGenerator(Path privateKey) {
        return new AppleClientSecretGenerator(CLIENT_ID, TEAM_ID, KEY_ID, privateKey.toString(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private KeyPair createKeyPair(String curve) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    private Path writeKey(KeyPair keyPair) throws Exception {
        Path privateKey = temporaryDirectory.resolve("AuthKey_Test.p8");
        String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        Files.writeString(privateKey, "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n");
        return privateKey;
    }

    private IllegalStateException assertSanitizedFailure(AppleClientSecretGenerator generator) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, generator::generate);
        assertNull(exception.getCause());
        assertFalse(exception.getMessage().contains(temporaryDirectory.toString()));
        return exception;
    }
}
