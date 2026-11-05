package com.umc.bscene.global.security.oauth.apple;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public final class AppleIdTokenVerifier {

    private static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    private static final String ISSUER = "https://appleid.apple.com";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final JWTProcessor<SecurityContext> tokenProcessor;
    private final String clientId;
    private final Clock clock;

    public AppleIdTokenVerifier(String clientId) {
        this(createTokenProcessor(
                toUrl(APPLE_JWKS_URI),
                new DefaultResourceRetriever(2_000, 3_000, 65_536),
                Duration.ofMinutes(5).toMillis(),
                Duration.ofSeconds(30).toMillis()
        ), clientId, Clock.systemUTC());
    }

    AppleIdTokenVerifier(JWTProcessor<SecurityContext> tokenProcessor, String clientId, Clock clock) {
        this.tokenProcessor = tokenProcessor;
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clock = clock;
    }

    public VerifiedSocialIdentity verify(String identityToken, String expectedNonce) {
        return verify(identityToken, expectedNonce, null);
    }

    public VerifiedSocialIdentity verify(String identityToken, String expectedNonce, String authorizationCode) {
        JWTClaimsSet claims = verifyClaims(identityToken, true);
        validateNonce(claims, expectedNonce);
        validateAuthorizationCode(claims, authorizationCode);
        return createIdentity(claims);
    }

    // 만료 후에도 실패 정리의 소유자만 확인한다. 로그인 인증에는 반드시 nonce·현재 유효기간 검증을 사용한다.
    public VerifiedSocialIdentity verifyForCleanup(String identityToken) {
        return createIdentity(verifyClaims(identityToken, false));
    }

    private JWTClaimsSet verifyClaims(String identityToken, boolean requireCurrentTime) {
        validateRequest(identityToken);
        try {
            JWTClaimsSet claims = processToken(identityToken);
            validateIdentity(claims);
            if (requireCurrentTime) {
                validateTime(claims);
            }
            validateAudience(claims);
            return claims;
        } catch (KeySourceException exception) {
            throw new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
        } catch (ParseException | BadJOSEException | JOSEException exception) {
            // 토큰을 포함할 수 있는 제공자 예외 메시지·cause를 응답/로그에 넘기지 않는다.
            throw createInvalidTokenException();
        }
    }

    private VerifiedSocialIdentity createIdentity(JWTClaimsSet claims) {
        try {
            return new VerifiedSocialIdentity(SocialProvider.APPLE, claims.getSubject(),
                    claims.getStringClaim("email"), null);
        } catch (ParseException exception) {
            throw createInvalidTokenException();
        }
    }

    private void validateRequest(String identityToken) {
        if (clientId.isBlank()) {
            throw new IllegalStateException("Apple 모바일 OAuth client ID 설정이 필요합니다.");
        }
        if (identityToken == null || identityToken.isBlank()) {
            throw createInvalidTokenException();
        }
    }

    private JWTClaimsSet processToken(String identityToken) throws ParseException, BadJOSEException, JOSEException {
        SignedJWT signedToken = SignedJWT.parse(identityToken);
        if (!JWSAlgorithm.RS256.equals(signedToken.getHeader().getAlgorithm())
                || signedToken.getHeader().getKeyID() == null || signedToken.getHeader().getKeyID().isBlank()) {
            throw createInvalidTokenException();
        }
        return tokenProcessor.process(signedToken, null);
    }

    private void validateIdentity(JWTClaimsSet claims) {
        String subject = claims.getSubject();
        if (!ISSUER.equals(claims.getIssuer())
                || subject == null || subject.isBlank() || subject.length() > 255
                || !subject.chars().allMatch(character -> character >= 0x21 && character <= 0x7e)) {
            throw createInvalidTokenException();
        }
    }

    private void validateTime(JWTClaimsSet claims) {
        Instant now = clock.instant();
        Date expiration = claims.getExpirationTime();
        Date issuedAt = claims.getIssueTime();
        Date notBefore = claims.getNotBeforeTime();
        if (expiration == null || issuedAt == null
                || !expiration.toInstant().plus(CLOCK_SKEW).isAfter(now)
                || issuedAt.toInstant().isAfter(now.plus(CLOCK_SKEW))
                || !expiration.after(issuedAt)
                || (notBefore != null && notBefore.toInstant().isAfter(now.plus(CLOCK_SKEW)))) {
            throw createInvalidTokenException();
        }
    }

    private void validateAudience(JWTClaimsSet claims) throws ParseException {
        List<String> audiences = claims.getAudience();
        if (audiences == null || audiences.isEmpty()
                || audiences.stream().anyMatch(audience -> audience == null || audience.isBlank())) {
            throw createInvalidTokenException();
        }
        if (!List.of(clientId).equals(audiences)) {
            throw new OauthException(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
        }
        if (claims.getClaims().containsKey("azp") && !clientId.equals(claims.getStringClaim("azp"))) {
            throw new OauthException(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
        }
    }

    private void validateNonce(JWTClaimsSet claims, String expectedNonce) {
        Object nonce = claims.getClaim("nonce");
        if (!(nonce instanceof String nonceValue) || nonceValue.isBlank()
                || expectedNonce == null || expectedNonce.isBlank() || !isEqual(nonceValue, expectedNonce)) {
            throw new OauthException(OauthErrorCode.APPLE_NONCE_MISMATCH);
        }
    }

    private void validateAuthorizationCode(JWTClaimsSet claims, String authorizationCode) {
        Object codeHash = claims.getClaim("c_hash");
        if (codeHash == null && !claims.getClaims().containsKey("c_hash")) {
            return;
        }
        if (!(codeHash instanceof String hashValue) || hashValue.isBlank()
                || authorizationCode == null || authorizationCode.isBlank()
                || !authorizationCode.chars().allMatch(character -> character >= 0x21 && character <= 0x7e)
                || !isEqual(hashValue, createCodeHash(authorizationCode))) {
            throw createInvalidTokenException();
        }
    }

    private String createCodeHash(String authorizationCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(authorizationCode.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, digest.length / 2));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Apple 인가 코드 검증용 해시를 사용할 수 없습니다.");
        }
    }

    private boolean isEqual(String actual, String expected) {
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    static JWTProcessor<SecurityContext> createTokenProcessor(
            URL keySetUrl, ResourceRetriever resourceRetriever,
            long cacheTimeToLiveMilliseconds, long refreshIntervalMilliseconds
    ) {
        JWKSource<SecurityContext> keySource = createKeySource(
                keySetUrl, resourceRetriever, cacheTimeToLiveMilliseconds, refreshIntervalMilliseconds
        );
        var tokenProcessor = new DefaultJWTProcessor<SecurityContext>();
        tokenProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
        tokenProcessor.setJWTClaimsSetVerifier((claims, context) -> { });
        return tokenProcessor;
    }

    private static JWKSource<SecurityContext> createKeySource(
            URL keySetUrl, ResourceRetriever resourceRetriever,
            long cacheTimeToLiveMilliseconds, long refreshIntervalMilliseconds
    ) {
        var sourceBuilder = JWKSourceBuilder.<SecurityContext>create(keySetUrl, resourceRetriever)
                .cache(cacheTimeToLiveMilliseconds, 3_000)
                .refreshAheadCache(false)
                .retrying(false)
                .outageTolerant(false);
        if (refreshIntervalMilliseconds > 0) {
            sourceBuilder.rateLimited(refreshIntervalMilliseconds);
        } else {
            sourceBuilder.rateLimited(false);
        }
        return sourceBuilder.build();
    }

    private static URL toUrl(String uri) {
        try {
            return URI.create(uri).toURL();
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalStateException("Apple 공개키 주소 설정이 올바르지 않습니다.");
        }
    }

    private static OauthException createInvalidTokenException() {
        return new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
    }
}
