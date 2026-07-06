package com.umc.bscene.global.security.util;

import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;
    private final Duration signupExpiration;

    public JwtUtil(
            @Value("${jwt.token.secretKey}") String secret,
            @Value("${jwt.token.expiration.access}") Long accessExpiration,
            @Value("${jwt.token.expiration.refresh}") Long refreshExpiration,
            @Value("${jwt.token.expiration.signup}") Long signupExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMillis(accessExpiration);
        this.refreshExpiration = Duration.ofMillis(refreshExpiration);
        this.signupExpiration = Duration.ofMillis(signupExpiration);
    }

    // AccessToken 생성
    public String createAccessToken(AuthMember member) {
        return createToken(member, accessExpiration, "access");
    }

    // RefreshToken 생성
    public String createRefreshToken(AuthMember member) {
        return createToken(member, refreshExpiration, "refresh");
    }

    // 소셜 신규 유저용 임시 회원가입 토큰 생성 (provider/providerUid/name/email 보관)
    public String createSignupToken(String provider, String providerUid, String name, String email) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(providerUid)
                .claim("type", "signup")
                .claim("provider", provider)
                .claim("providerUid", providerUid)
                .claim("name", name)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(signupExpiration)))
                .signWith(secretKey)
                .compact();
    }

    /** 토큰에서 userId 가져오기
     *
     * @param token 유저 정보를 추출할 토큰
     * @return 토큰 subject에 담긴 userId를 반환합니다
     */
    public String getUserId(String token) {
        try {
            return getClaims(token).getPayload().getSubject(); // Parsing해서 Subject(userId) 가져오기
        } catch (JwtException e) {
            return null;
        }
    }

    /** 토큰 종류(access/refresh) 가져오기
     *
     * @param token type claim을 추출할 토큰
     * @return "access" 또는 "refresh", 파싱 실패 시 null
     */
    public String getType(String token) {
        try {
            return getClaims(token).getPayload().get("type", String.class);
        } catch (JwtException e) {
            return null;
        }
    }

    // signup 토큰에서 provider 추출
    public String getProvider(String token) {
        return getClaims(token).getPayload().get("provider", String.class);
    }

    // signup 토큰에서 providerUid 추출
    public String getProviderUid(String token) {
        return getClaims(token).getPayload().get("providerUid", String.class);
    }

    // signup 토큰에서 name 추출
    public String getName(String token) {
        return getClaims(token).getPayload().get("name", String.class);
    }

    // signup 토큰에서 email(소셜 제공자 이메일 = 아이디) 추출
    public String getEmail(String token) {
        return getClaims(token).getPayload().get("email", String.class);
    }

    /** 토큰 유효성 확인
     *
     * @param token 유효한지 확인할 토큰
     * @return True, False 반환합니다
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // 토큰 생성
    private String createToken(AuthMember member, Duration expiration, String type) {
        Instant now = Instant.now();

        // 인가 정보
        String authorities = member.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(member.getUsername()) // userId를 Subject로
                .claim("role", authorities)
                .claim("type", type) // access / refresh 구분
                .issuedAt(Date.from(now)) // 언제 발급한지
                .expiration(Date.from(now.plus(expiration))) // 언제까지 유효한지
                .signWith(secretKey) // sign할 Key
                .compact();
    }

    // 토큰 정보 가져오기
    private Jws<Claims> getClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token);
    }

    // AccessToken 만료 시간 반환
    public Long getAccessTokenExpiration() {
        return accessExpiration.toMillis();
    }

    // RefreshToken 만료 시간 반환
    public Long getRefreshTokenExpiration() {
        return refreshExpiration.toMillis();
    }

    /**
     * AccessToken을 검증하고 subject에 담긴 userId를 반환
     *
     * @param token 검증할 AccessToken
     * @return 토큰 subject에 담긴 userId
     */
    public String validateAccessTokenAndGetUserId(String token) {
        try {
            Jws<Claims> claims = getClaims(token);
            String type = claims.getPayload().get("type", String.class);

            if (!"access".equals(type)) {
                throw new JwtAuthenticationException(GeneralErrorCode.INVALID_ACCESS_TOKEN);
            }

            return claims.getPayload().getSubject();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(GeneralErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(GeneralErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
