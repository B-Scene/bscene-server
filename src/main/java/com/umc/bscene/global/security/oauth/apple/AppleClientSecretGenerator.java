package com.umc.bscene.global.security.oauth.apple;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public final class AppleClientSecretGenerator {

    private static final Duration SECRET_LIFETIME = Duration.ofMinutes(5);
    private static final int MAX_KEY_BYTES = 16_384;
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String PEM_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_END = "-----END PRIVATE KEY-----";

    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final String privateKeyPath;
    private final Clock clock;

    public AppleClientSecretGenerator(String clientId, String teamId, String keyId, String privateKeyPath) {
        this(clientId, teamId, keyId, privateKeyPath, Clock.systemUTC());
    }

    AppleClientSecretGenerator(String clientId, String teamId, String keyId, String privateKeyPath, Clock clock) {
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKeyPath = privateKeyPath;
        this.clock = clock;
    }

    public String generate() {
        validateConfiguration();
        try {
            SignedJWT clientSecret = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), createClaims());
            clientSecret.sign(new ECDSASigner(readPrivateKey()));
            return clientSecret.serialize();
        } catch (IOException | GeneralSecurityException | JOSEException | IllegalArgumentException exception) {
            // 경로·키 원문이 담길 수 있는 예외 cause를 로그/응답 경로로 넘기지 않는다.
            throw new IllegalStateException("Apple OAuth 서명 키 설정을 확인해 주세요.");
        }
    }

    public void validateConfiguration() {
        if (clientId == null || clientId.isBlank() || !clientId.equals(clientId.trim())
                || teamId == null || !teamId.matches("[A-Z0-9]{10}")
                || keyId == null || !keyId.matches("[A-Z0-9]{10}")
                || privateKeyPath == null || privateKeyPath.isBlank()) {
            throw new IllegalStateException("Apple OAuth client ID·Team ID·Key ID·서명 키 설정이 필요합니다.");
        }
    }

    private JWTClaimsSet createClaims() {
        Instant issuedAt = clock.instant();
        return new JWTClaimsSet.Builder()
                .issuer(teamId)
                .subject(clientId)
                .audience(APPLE_ISSUER)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(SECRET_LIFETIME)))
                .build();
    }

    private ECPrivateKey readPrivateKey() throws IOException, GeneralSecurityException {
        String pem;
        try (var input = Files.newInputStream(Path.of(privateKeyPath))) {
            byte[] encodedKey = input.readNBytes(MAX_KEY_BYTES + 1);
            if (encodedKey.length > MAX_KEY_BYTES) {
                throw new IllegalArgumentException();
            }
            pem = new String(encodedKey, StandardCharsets.US_ASCII).trim();
        }
        if (!pem.startsWith(PEM_BEGIN) || !pem.endsWith(PEM_END)) {
            throw new IllegalArgumentException();
        }
        String encodedKey = pem.substring(PEM_BEGIN.length(), pem.length() - PEM_END.length()).replaceAll("\\s", "");
        var keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
        ECPrivateKey privateKey = (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(keySpec);
        validateCurve(privateKey);
        return privateKey;
    }

    private void validateCurve(ECPrivateKey privateKey) {
        var expected = Curve.P_256.toECParameterSpec();
        var actual = privateKey.getParams();
        if (!expected.getCurve().equals(actual.getCurve())
                || !expected.getGenerator().equals(actual.getGenerator())
                || !expected.getOrder().equals(actual.getOrder())
                || expected.getCofactor() != actual.getCofactor()) {
            throw new IllegalArgumentException();
        }
    }
}
