package com.umc.bscene.domain.auth.service.auth;

import com.umc.bscene.domain.auth.dto.auth.request.*;
import com.umc.bscene.domain.auth.dto.auth.response.*;
import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    // 로그인
    @Transactional
    public TokenResponse login(LoginRequest request) {
        LocalCredential localCredential = localCredentialRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), localCredential.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.LOGIN_FAILED);
        }

        User user = localCredential.getUser();
        validateUserStatus(user);

        AuthMember authMember = new AuthMember(user);

        String accessToken = jwtUtil.createAccessToken(authMember);
        String refreshToken = jwtUtil.createRefreshToken(authMember);

        String refreshTokenHash = hashToken(refreshToken);

        stringRedisTemplate.opsForValue().set(
                "refreshToken:" + refreshTokenHash,
                String.valueOf(user.getId()),
                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
        );

        LoginUserResponse loginUserResponse = new LoginUserResponse(
                user.getId(),
                user.getName(),
                user.getCurrentMode(),
                user.getOnboardingCompleted()
        );

        return new TokenResponse(
                "Bearer",
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                loginUserResponse
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashedToken = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedToken);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("토큰 해시에 실패했습니다.", e);
        }
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(AuthErrorCode.SUSPENDED_ACCOUNT);
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AuthException(AuthErrorCode.INACTIVE_ACCOUNT);
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException(AuthErrorCode.DELETED_ACCOUNT);
        }
    }

    // Reissue로 인한 AccessToken, RefreshToken 재발급
    public ReissueResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtil.isValid(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!"refresh".equals(jwtUtil.getType(refreshToken))) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String refreshTokenHash = hashToken(refreshToken);
        String userId = stringRedisTemplate.opsForValue().get("refreshToken:" + refreshTokenHash);

        if (userId == null) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        // 정지/휴면/탈퇴 계정은 로그인과 동일하게 재발급도 차단하고, 보유한 리프레시 토큰은 폐기한다
        if (user.getStatus() != UserStatus.ACTIVE) {
            stringRedisTemplate.delete("refreshToken:" + refreshTokenHash);
            validateUserStatus(user);
        }

        AuthMember authMember = new AuthMember(user);
        String newAccessToken = jwtUtil.createAccessToken(authMember);
        String newRefreshToken = jwtUtil.createRefreshToken(authMember);

        stringRedisTemplate.delete("refreshToken:" + refreshTokenHash);

        String newRefreshTokenHash = hashToken(newRefreshToken);
        stringRedisTemplate.opsForValue().set(
                "refreshToken:" + newRefreshTokenHash,
                String.valueOf(user.getId()),
                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
        );

        return ReissueResponse.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    // 로그아웃
    public void logout(LogoutRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtil.isValid(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!"refresh".equals(jwtUtil.getType(refreshToken))) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String refreshTokenHash = hashToken(refreshToken);
        Boolean deleted = stringRedisTemplate.delete("refreshToken:" + refreshTokenHash);

        if (!Boolean.TRUE.equals(deleted)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}