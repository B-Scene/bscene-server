package com.umc.bscene.global.security.util;

import com.umc.bscene.global.security.entity.AuthMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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

    public JwtUtil(
            @Value("${jwt.token.secretKey}") String secret,
            @Value("${jwt.token.expiration.access}") Long accessExpiration,
            @Value("${jwt.token.expiration.refresh}") Long refreshExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMillis(accessExpiration);
        this.refreshExpiration = Duration.ofMillis(refreshExpiration);
    }

    // AccessToken 생성
    public String createAccessToken(AuthMember member) {
        return createToken(member, accessExpiration, "access");
    }

    // RefreshToken 생성
    public String createRefreshToken(AuthMember member) {
        return createToken(member, refreshExpiration, "refresh");
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
}
