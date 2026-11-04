package com.umc.bscene.global.security.oauth.google;

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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleIdTokenVerifierTest {

    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
    private static final String CLIENT_ID = "bscene-server.apps.googleusercontent.com";
    private static RSAKey signingKey;
    private static RSAKey rotatedKey;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> jwksResponse = new AtomicReference<>();
    private HttpServer server;
    private GoogleIdTokenVerifier verifier;

    @BeforeAll
    static void createKeys() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID("google-key-1").generate();
        rotatedKey = new RSAKeyGenerator(2048).keyID("google-key-2").generate();
    }

    @BeforeEach
    void setUp() throws Exception {
        jwksResponse.set(new JWKSet(signingKey.toPublicJWK()).toString());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/certs", exchange -> {
            requestCount.incrementAndGet();
            byte[] response = jwksResponse.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus.get(), response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        verifier = createVerifier(Set.of(CLIENT_ID), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void verifiesSignatureAndReturnsProviderIdentity() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims()
                .claim("email", "member@example.com")
                .claim("name", "Member")
                .build()));

        assertEquals(SocialProvider.GOOGLE, identity.provider());
        assertEquals("google-subject", identity.providerUid());
        assertEquals("member@example.com", identity.email());
        assertEquals("Member", identity.name());
    }

    @Test
    void permitsMissingEmailAndNameForCommonMemberLookup() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims().build()));

        assertNull(identity.email());
        assertNull(identity.name());
    }

    @Test
    void acceptsGoogleLegacyIssuer() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims().issuer("accounts.google.com").build()));

        assertEquals("google-subject", identity.providerUid());
    }

    @Test
    void rejectsUnsignedMalformedAndBlankTokensWithoutKeyRequests() {
        for (String input : List.of("", " ", "not-a-jwt", "e30.e30.")) {
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, input);
        }
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, null);
        assertEquals(0, requestCount.get());
    }

    @Test
    void rejectsDifferentSignature() throws Exception {
        RSAKey wrongKey = new RSAKeyGenerator(2048).keyID(signingKey.getKeyID()).generate();

        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, createToken(wrongKey, createClaims().build()));
    }

    @Test
    void rejectsUnexpectedAlgorithmBeforeFetchingKeys() throws Exception {
        SignedJWT signedToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(signingKey.getKeyID()).build(), createClaims().build());
        signedToken.sign(new RSASSASigner(signingKey));

        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, signedToken.serialize());
        assertEquals(0, requestCount.get());
    }

    @Test
    void rejectsMissingRequiredIdentityAndTimeClaims() throws Exception {
        List<JWTClaimsSet> invalidClaims = List.of(
                createClaims().issuer(null).build(),
                createClaims().issuer("https://attacker.example").build(),
                createClaims().subject(null).build(),
                createClaims().subject(" ").build(),
                createClaims().expirationTime(null).build(),
                createClaims().issueTime(null).build()
        );
        for (JWTClaimsSet claimSet : invalidClaims) {
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, createToken(signingKey, claimSet));
        }
    }

    @Test
    void rejectsExpiredFutureAndInconsistentTimes() throws Exception {
        List<JWTClaimsSet> invalidClaims = List.of(
                createClaims().issueTime(Date.from(NOW.minusSeconds(600)))
                        .expirationTime(Date.from(NOW.minusSeconds(120))).build(),
                createClaims().issueTime(Date.from(NOW.plusSeconds(120))).build(),
                createClaims().notBeforeTime(Date.from(NOW.plusSeconds(120))).build(),
                createClaims().expirationTime(Date.from(NOW.minusSeconds(40))).build()
        );
        for (JWTClaimsSet claimSet : invalidClaims) {
            assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier, createToken(signingKey, claimSet));
        }
    }

    @Test
    void rejectsWrongAudienceWithAppMismatchCode() throws Exception {
        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, verifier,
                createToken(signingKey, createClaims().audience("another-client").build()));
    }

    @Test
    void rejectsMissingAudienceAsInvalidToken() throws Exception {
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(signingKey, createClaims().audience(List.of()).build()));
    }

    @Test
    void acceptsDifferentMobilePresenterForSingleServerAudience() throws Exception {
        var identity = verifier.verify(createToken(signingKey, createClaims()
                .claim("azp", "mobile-client.apps.googleusercontent.com").build()));

        assertEquals("google-subject", identity.providerUid());
    }

    @Test
    void requiresTrustedAudiencesAndPresenterForMultipleAudiences() throws Exception {
        String mobileClientId = "mobile-client.apps.googleusercontent.com";
        var multiAudienceVerifier = createVerifier(Set.of(CLIENT_ID, mobileClientId), 0);
        var multiAudience = createClaims().audience(List.of(CLIENT_ID, mobileClientId));

        var identity = multiAudienceVerifier.verify(createToken(signingKey,
                multiAudience.claim("azp", mobileClientId).build()));
        assertEquals("google-subject", identity.providerUid());

        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, multiAudienceVerifier,
                createToken(signingKey, createClaims().audience(List.of(CLIENT_ID, mobileClientId)).build()));
        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, multiAudienceVerifier,
                createToken(signingKey, createClaims().audience(List.of(CLIENT_ID, mobileClientId))
                        .claim("azp", "untrusted-client").build()));
        assertError(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, multiAudienceVerifier,
                createToken(signingKey, createClaims().audience(List.of(CLIENT_ID, "untrusted-client"))
                        .claim("azp", mobileClientId).build()));
    }

    @Test
    void rejectsBlankAndWrongTypePresenter() throws Exception {
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(signingKey, createClaims().claim("azp", " ").build()));
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, verifier,
                createToken(signingKey, createClaims().claim("azp", 123).build()));
    }

    @Test
    void cachesKeysAcrossRequests() throws Exception {
        String idToken = createToken(signingKey, createClaims().build());

        verifier.verify(idToken);
        verifier.verify(idToken);

        assertEquals(1, requestCount.get());
    }

    @Test
    void refreshesKeysOnRotation() throws Exception {
        verifier.verify(createToken(signingKey, createClaims().build()));
        jwksResponse.set(new JWKSet(rotatedKey.toPublicJWK()).toString());

        var identity = verifier.verify(createToken(rotatedKey, createClaims().build()));

        assertEquals("google-subject", identity.providerUid());
        assertEquals(2, requestCount.get());
    }

    @Test
    void limitsRepeatedUnknownKeyRefreshes() throws Exception {
        var rateLimitedVerifier = createVerifier(Set.of(CLIENT_ID), 60_000);
        rateLimitedVerifier.verify(createToken(signingKey, createClaims().build()));
        String unknownKeyToken = createToken(rotatedKey, createClaims().build());

        for (int attempt = 0; attempt < 10; attempt++) {
            assertThrows(OauthException.class, () -> rateLimitedVerifier.verify(unknownKeyToken));
        }

        assertTrue(requestCount.get() <= 2);
        assertEquals("google-subject", rateLimitedVerifier
                .verify(createToken(signingKey, createClaims().build())).providerUid());
    }

    @Test
    void mapsKeyServerFailuresToProviderUnavailableWithoutTokenCause() throws Exception {
        responseStatus.set(503);
        String idToken = createToken(signingKey, createClaims().build());

        OauthException exception = assertThrows(OauthException.class, () -> verifier.verify(idToken));

        assertEquals(OauthErrorCode.PROVIDER_UNAVAILABLE, exception.getBaseResponseCode());
        assertNull(exception.getCause());
    }

    @Test
    void rejectsMalformedJwksAsProviderUnavailable() throws Exception {
        jwksResponse.set("not-json");

        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, verifier, createToken(signingKey, createClaims().build()));
    }

    @Test
    void failsClosedOnEmptyConfigurationBeforeFetchingKeys() {
        var unconfigured = createVerifier(Set.of(), 0);

        assertThrows(IllegalStateException.class, () -> unconfigured.verify("not-a-token"));
        assertEquals(0, requestCount.get());
    }

    private GoogleIdTokenVerifier createVerifier(Set<String> audiences, long refreshIntervalMillis) {
        try {
            var processor = GoogleIdTokenVerifier.createTokenProcessor(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/certs").toURL(),
                    new DefaultResourceRetriever(500, 500, 65_536),
                    300_000,
                    refreshIntervalMillis
            );
            return new GoogleIdTokenVerifier(processor, audiences, Clock.fixed(NOW, ZoneOffset.UTC));
        } catch (Exception exception) {
            throw new IllegalStateException("테스트용 Google 검증기 생성에 실패했습니다.", exception);
        }
    }

    private static JWTClaimsSet.Builder createClaims() {
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .subject("google-subject")
                .audience(CLIENT_ID)
                .issueTime(Date.from(NOW.minusSeconds(30)))
                .expirationTime(Date.from(NOW.plusSeconds(300)));
    }

    private static String createToken(RSAKey key, JWTClaimsSet claimSet) throws JOSEException {
        SignedJWT signedToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID()).build(), claimSet);
        signedToken.sign(new RSASSASigner(key));
        return signedToken.serialize();
    }

    private static void assertError(OauthErrorCode expected, GoogleIdTokenVerifier verifier, String token) {
        OauthException exception = assertThrows(OauthException.class, () -> verifier.verify(token));
        assertEquals(expected, exception.getBaseResponseCode());
        assertNull(exception.getCause());
    }
}
