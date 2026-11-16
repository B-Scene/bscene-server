package com.umc.bscene.global.security.util;

import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 토큰 발급 단위테스트 (Refresh Token 발급별 고유성)
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-util-test-0123456789abcdef";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L, 1_209_600_000L, 600_000L);
    private final AuthMember member = new AuthMember(StreamFixtures.user(1L, UserMode.FAN));

    @Test
    void createRefreshToken_같은_회원에게_연속으로_발급해도_토큰이_모두_다르다() {
        // 발급 시각(iat)·만료 시각(exp)은 초 단위라, jti 가 없으면 같은 초에 발급한 토큰은 같은 문자열이 된다
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            tokens.add(jwtUtil.createRefreshToken(member));
        }

        assertEquals(20, tokens.size());
    }

    @Test
    void createRefreshToken_발급마다_jti가_다르다() {
        String first = jwtUtil.createRefreshToken(member);
        String second = jwtUtil.createRefreshToken(member);

        assertNotEquals(jwtUtil.getSignupId(first), jwtUtil.getSignupId(second));
    }

    @Test
    void createRefreshToken_jti를_넣어도_기존_검증과_조회는_그대로_동작한다() {
        String refreshToken = jwtUtil.createRefreshToken(member);

        assertTrue(jwtUtil.isValid(refreshToken));
        assertEquals("refresh", jwtUtil.getType(refreshToken));
        assertEquals("1", jwtUtil.getUserId(refreshToken));
    }

    @Test
    void createAccessToken_jti를_넣지_않는다() {
        String accessToken = jwtUtil.createAccessToken(member);

        assertTrue(jwtUtil.isValid(accessToken));
        assertEquals("access", jwtUtil.getType(accessToken));
        assertNull(jwtUtil.getSignupId(accessToken));
    }
}
