package com.umc.bscene.global.security.oauth.apple;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleIdTokenVerifierTest {

    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");
    private static final String CLIENT_ID = "com.bscene.mobile";
    private static final String NONCE = "server-issued-raw-nonce";
    private static final String AUTHORIZATION_CODE = "apple-authorization-code";
    private static RSAKey signingKey;
    private static RSAKey rotatedKey;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> jwksResponse = new AtomicReference<>();
    private HttpServer server;
    private AppleIdTokenVerifier verifier;

    @BeforeAll
    static void createKeys() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID("apple-key-1").generate();
        rotatedKey = new RSAKeyGenerator(2048).keyID("apple-key-2").generate();
    }

    @BeforeEach
    void setUp() throws Exception {
        jwksResponse.set(new JWKSet(signingKey.toPublicJWK()).toString());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/keys", exchange -> {
            requestCount.incrementAndGet();
            byte[] response = jwksResponse.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus.get(), response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        verifier = createVerifier(CLIENT_ID, 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void verifiesSignatureAndRawNonceAndReturnsAppleIdentity() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims()
                .claim("email", "member@example.com").build()), NONCE);

        assertEquals(SocialProvider.APPLE, identity.provider());
        assertEquals("apple-subject", identity.providerUid());
        assertEquals("member@example.com", identity.email());
        assertNull(identity.name());
    }

    @Test
    void acceptsPrivateRelayEmailWithoutUsingItAsUserIdentifier() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims()
                .claim("email", "relay@privaterelay.appleid.com").build()), NONCE);

        assertEquals("relay@privaterelay.appleid.com", identity.email());
        assertEquals("apple-subject", identity.providerUid());
    }

    @Test
    void permitsMissingEmailAndNameForExistingMemberLookup() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims().build()), NONCE);

        assertNull(identity.email());
        assertNull(identity.name());
    }

    @Test
    void rejectsUnsignedMalformedAndBlankTokensWithoutKeyRequests() {
        for (String input : List.of("", " ", "not-a-jwt", "e30.e30.")) {
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, input, NONCE);
        }
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, null, NONCE);
        assertEquals(0, requestCount.get());
    }

    @Test
    void rejectsDifferentSignature() throws Exception {
        RSAKey wrongKey = new RSAKeyGenerator(2048).keyID(signingKey.getKeyID()).generate();

        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(wrongKey, createClaims().build()), NONCE);
    }

    @Test
    void rejectsUnexpectedAlgorithmBeforeFetchingKeys() throws Exception {
        SignedJWT signedToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(signingKey.getKeyID()).build(), createClaims().build());
        signedToken.sign(new RSASSASigner(signingKey));

        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, signedToken.serialize(), NONCE);
        assertEquals(0, requestCount.get());
    }

    @Test
    void rejectsMissingKeyIdentifierBeforeFetchingKeys() throws Exception {
        SignedJWT signedToken = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), createClaims().build());
        signedToken.sign(new RSASSASigner(signingKey));

        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, signedToken.serialize(), NONCE);
        assertEquals(0, requestCount.get());
    }

    @Test
    void rejectsMissingRequiredIdentityAndTimeClaims() throws Exception {
        List<JWTClaimsSet> invalidClaims = List.of(
                createClaims().issuer(null).build(),
                createClaims().issuer("https://attacker.example").build(),
                createClaims().subject(null).build(),
                createClaims().subject(" ").build(),
                createClaims().subject("x".repeat(256)).build(),
                createClaims().subject("apple-사용자").build(),
                createClaims().expirationTime(null).build(),
                createClaims().issueTime(null).build()
        );
        for (JWTClaimsSet claimSet : invalidClaims) {
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, createToken(signingKey, claimSet), NONCE);
        }
    }

    @Test
    void rejectsExpiredFutureAndInconsistentTimes() throws Exception {
        List<JWTClaimsSet> invalidClaims = List.of(
                createClaims().issueTime(Date.from(NOW.minusSeconds(600)))
                        .expirationTime(Date.from(NOW.minusSeconds(60))).build(),
                createClaims().issueTime(Date.from(NOW.plusSeconds(120))).build(),
                createClaims().notBeforeTime(Date.from(NOW.plusSeconds(120))).build(),
                createClaims().expirationTime(Date.from(NOW.minusSeconds(40))).build()
        );
        for (JWTClaimsSet claimSet : invalidClaims) {
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, createToken(signingKey, claimSet), NONCE);
        }
    }

    @Test
    void rejectsWrongOrAdditionalAudienceWithAppMismatchCode() throws Exception {
        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, verifier,
                createToken(signingKey, createClaims().audience("another-client").build()), NONCE);
        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, verifier,
                createToken(signingKey, createClaims().audience(List.of(CLIENT_ID, "another-client")).build()), NONCE);
    }

    @Test
    void rejectsMissingAudienceAsInvalidToken() throws Exception {
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(signingKey, createClaims().audience(List.of()).build()), NONCE);
    }

    @Test
    void checksAuthorizedPartyWhenPresent() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims().claim("azp", CLIENT_ID).build()), NONCE);

        assertEquals("apple-subject", identity.providerUid());
        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, verifier,
                createToken(signingKey, createClaims().claim("azp", "another-client").build()), NONCE);
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(signingKey, createClaims().claim("azp", 123).build()), NONCE);
    }

    @Test
    void rejectsMissingWrongTypeAndDifferentNonces() throws Exception {
        for (Object nonce : List.of("", "another-nonce", 123)) {
            assertError(OauthErrorCode.APPLE_NONCE_MISMATCH, verifier,
                    createToken(signingKey, createClaims().claim("nonce", nonce).build()), NONCE);
        }
        assertError(OauthErrorCode.APPLE_NONCE_MISMATCH, verifier,
                createToken(signingKey, createClaims().claim("nonce", null).build()), NONCE);
        String token = createToken(signingKey, createClaims().build());
        assertError(OauthErrorCode.APPLE_NONCE_MISMATCH, verifier, token, null);
        assertError(OauthErrorCode.APPLE_NONCE_MISMATCH, verifier, token, " ");
    }

    @Test
    void rejectsHashedNonceWhenContractRequiresRawNonce() throws Exception {
        String hashedNonce = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(NONCE.getBytes(StandardCharsets.UTF_8)));

        assertError(OauthErrorCode.APPLE_NONCE_MISMATCH, verifier,
                createToken(signingKey, createClaims().claim("nonce", hashedNonce).build()), NONCE);
    }

    @Test
    void verifiesAuthorizationCodeHashWhenPresent() throws Exception {
        String token = createToken(signingKey, createClaims()
                .claim("c_hash", createCodeHash(AUTHORIZATION_CODE)).build());

        assertEquals("apple-subject", verifier.verify(token, NONCE, AUTHORIZATION_CODE).providerUid());
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, token, NONCE, "different-code");
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, token, NONCE);
    }

    @Test
    void permitsAbsentCodeHashOnTokenEndpointResponse() throws Exception {
        String token = createToken(signingKey, createClaims().build());

        assertEquals("apple-subject", verifier.verify(token, NONCE, AUTHORIZATION_CODE).providerUid());
    }

    @Test
    void rejectsMalformedCodeHashAndNonAsciiAuthorizationCode() throws Exception {
        for (Object hash : List.of("", "not-the-code-hash", 123)) {
            String token = createToken(signingKey, createClaims().claim("c_hash", hash).build());
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, token, NONCE, AUTHORIZATION_CODE);
        }
        String token = createToken(signingKey, createClaims()
                .claim("c_hash", createCodeHash(AUTHORIZATION_CODE)).build());
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, token, NONCE, "인가코드");
    }

    @Test
    void doesNotAcceptNonceFromUnsignedToken() throws Exception {
        RSAKey wrongKey = new RSAKeyGenerator(2048).keyID(signingKey.getKeyID()).generate();

        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(wrongKey, createClaims().claim("nonce", "another-nonce").build()), NONCE);
    }

    @Test
    void rejectsWrongTypeEmailWithoutExposingTokenCause() throws Exception {
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(signingKey, createClaims().claim("email", 123).build()), NONCE);
    }

    @Test
    void cachesKeysAcrossRequests() throws Exception {
        String token = createToken(signingKey, createClaims().build());

        verifier.verify(token, NONCE);
        verifier.verify(token, NONCE);

        assertEquals(1, requestCount.get());
    }

    @Test
    void refreshesKeysOnRotation() throws Exception {
        verifier.verify(createToken(signingKey, createClaims().build()), NONCE);
        jwksResponse.set(new JWKSet(rotatedKey.toPublicJWK()).toString());

        var identity = verifier.verify(createToken(rotatedKey, createClaims().build()), NONCE);

        assertEquals("apple-subject", identity.providerUid());
        assertEquals(2, requestCount.get());
    }

    @Test
    void limitsRepeatedUnknownKeyRefreshes() throws Exception {
        var rateLimitedVerifier = createVerifier(CLIENT_ID, 60_000);
        rateLimitedVerifier.verify(createToken(signingKey, createClaims().build()), NONCE);
        String unknownKeyToken = createToken(rotatedKey, createClaims().build());

        for (int attempt = 0; attempt < 10; attempt++) {
            assertThrows(OauthException.class, () -> rateLimitedVerifier.verify(unknownKeyToken, NONCE));
        }

        assertTrue(requestCount.get() <= 2);
        assertEquals("apple-subject", rateLimitedVerifier
                .verify(createToken(signingKey, createClaims().build()), NONCE).providerUid());
    }

    @Test
    void mapsKeyServerFailuresToProviderUnavailableWithoutTokenCause() throws Exception {
        responseStatus.set(503);
        String token = createToken(signingKey, createClaims().build());

        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, verifier, token, NONCE);
    }

    @Test
    void rejectsMalformedJwksAsProviderUnavailable() throws Exception {
        jwksResponse.set("not-json");

        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, verifier,
                createToken(signingKey, createClaims().build()), NONCE);
    }

    @Test
    void failsClosedOnEmptyConfigurationBeforeFetchingKeys() {
        for (String clientId : List.of("", " ")) {
            var unconfigured = createVerifier(clientId, 0);
            assertThrows(IllegalStateException.class, () -> unconfigured.verify("not-a-token", NONCE));
        }
        assertEquals(0, requestCount.get());
    }

    private AppleIdTokenVerifier createVerifier(String clientId, long refreshIntervalMillis) {
        try {
            var processor = AppleIdTokenVerifier.createTokenProcessor(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/keys").toURL(),
                    new DefaultResourceRetriever(500, 500, 65_536), 300_000, refreshIntervalMillis
            );
            return new AppleIdTokenVerifier(processor, clientId, Clock.fixed(NOW, ZoneOffset.UTC));
        } catch (Exception exception) {
            throw new IllegalStateException("테스트용 Apple 검증기 생성에 실패했습니다.", exception);
        }
    }

    private static JWTClaimsSet.Builder createClaims() {
        return new JWTClaimsSet.Builder().issuer("https://appleid.apple.com")
                .subject("apple-subject").audience(CLIENT_ID)
                .issueTime(Date.from(NOW.minusSeconds(30)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .claim("nonce", NONCE);
    }

    private static String createToken(RSAKey key, JWTClaimsSet claimSet) throws JOSEException {
        SignedJWT signedToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID()).build(), claimSet);
        signedToken.sign(new RSASSASigner(key));
        return signedToken.serialize();
    }

    private static String createCodeHash(String code) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, digest.length / 2));
    }

    private static void assertError(
            OauthErrorCode expected, AppleIdTokenVerifier verifier, String token, String nonce
    ) {
        assertError(expected, verifier, token, nonce, null);
    }

    private static void assertError(
            OauthErrorCode expected, AppleIdTokenVerifier verifier, String token, String nonce, String code
    ) {
        OauthException exception = assertThrows(OauthException.class, () -> verifier.verify(token, nonce, code));
        assertEquals(expected, exception.getBaseResponseCode());
        assertNull(exception.getCause());
    }
}
