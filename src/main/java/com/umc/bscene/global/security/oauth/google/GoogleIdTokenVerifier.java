package com.umc.bscene.global.security.oauth.google;

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
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class GoogleIdTokenVerifier {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final JWTProcessor<SecurityContext> tokenProcessor;
    private final Set<String> allowedAudiences;
    private final Clock clock;

    public GoogleIdTokenVerifier(Set<String> allowedAudiences) {
        this(createTokenProcessor(
                toUrl(GOOGLE_JWKS_URI),
                new DefaultResourceRetriever(2_000, 3_000, 65_536),
                Duration.ofMinutes(5).toMillis(),
                Duration.ofSeconds(30).toMillis()
        ), allowedAudiences, Clock.systemUTC());
    }

    GoogleIdTokenVerifier(JWTProcessor<SecurityContext> tokenProcessor, Set<String> allowedAudiences, Clock clock) {
        this.tokenProcessor = tokenProcessor;
        this.allowedAudiences = Set.copyOf(allowedAudiences);
        this.clock = clock;
    }

    public VerifiedSocialIdentity verify(String idToken) {
        validateRequest(idToken);
        try {
            JWTClaimsSet verifiedClaims = processToken(idToken);
            validateIdentity(verifiedClaims);
            validateTime(verifiedClaims);
            validateAudience(verifiedClaims);
            return createIdentity(verifiedClaims);
        } catch (KeySourceException exception) {
            throw new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
        } catch (ParseException | BadJOSEException | JOSEException exception) {
            // 토큰을 포함할 수 있는 제공자 예외 메시지·cause를 응답/로그 경로로 넘기지 않는다.
            throw createInvalidTokenException();
        }
    }

    private void validateRequest(String idToken) {
        if (allowedAudiences.isEmpty()) {
            throw new IllegalStateException("Google 모바일 OAuth client ID 설정이 필요합니다.");
        }
        if (idToken == null || idToken.isBlank()) {
            throw createInvalidTokenException();
        }
    }

    private JWTClaimsSet processToken(String idToken) throws ParseException, BadJOSEException, JOSEException {
        SignedJWT signedToken = SignedJWT.parse(idToken);
        if (!JWSAlgorithm.RS256.equals(signedToken.getHeader().getAlgorithm())
                || signedToken.getHeader().getKeyID() == null || signedToken.getHeader().getKeyID().isBlank()) {
            throw createInvalidTokenException();
        }
        return tokenProcessor.process(signedToken, null);
    }

    private VerifiedSocialIdentity createIdentity(JWTClaimsSet claims) throws ParseException {
        return new VerifiedSocialIdentity(
                SocialProvider.GOOGLE,
                claims.getSubject(),
                claims.getStringClaim("email"),
                claims.getStringClaim("name")
        );
    }

    private void validateIdentity(JWTClaimsSet claims) {
        String issuer = claims.getIssuer();
        String subject = claims.getSubject();
        if (issuer == null || !ISSUERS.contains(issuer)
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
        String authorizedParty = claims.getStringClaim("azp");
        if (audiences == null || audiences.isEmpty()
                || audiences.stream().anyMatch(audience -> audience == null || audience.isBlank())
                || (claims.getClaims().containsKey("azp")
                    && (authorizedParty == null || authorizedParty.isBlank()))) {
            throw createInvalidTokenException();
        }
        if (!allowedAudiences.containsAll(audiences)) {
            throw new OauthException(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
        }

        // Google hybrid의 단일 aud와 azp는 다를 수 있다. 다중 aud만 신뢰 대상 전체와 azp를 제한한다.
        if (audiences.size() > 1 && (authorizedParty == null || !allowedAudiences.contains(authorizedParty))) {
            throw new OauthException(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
        }
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
        // 필수 claim·시각은 서명 검증 뒤 같은 Clock으로 확인한다.
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
            throw new IllegalStateException("Google 공개키 주소 설정이 올바르지 않습니다.");
        }
    }

    private static OauthException createInvalidTokenException() {
        return new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
    }
}
