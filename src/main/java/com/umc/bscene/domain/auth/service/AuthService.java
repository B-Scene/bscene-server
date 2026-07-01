package com.umc.bscene.domain.auth.service;

import com.umc.bscene.domain.auth.dto.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.request.SignupRequest;
import com.umc.bscene.domain.auth.dto.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.dto.response.LoginUserResponse;
import com.umc.bscene.domain.auth.dto.response.SignupResponse;
import com.umc.bscene.domain.auth.dto.response.TokenResponse;
import com.umc.bscene.domain.auth.entity.LocalCredential;
import com.umc.bscene.domain.auth.entity.RefreshToken;
import com.umc.bscene.domain.auth.exception.AuthException;
import com.umc.bscene.domain.auth.repository.LocalCredentialRepository;
import com.umc.bscene.domain.auth.repository.RefreshTokenRepository;
import com.umc.bscene.domain.auth.response.code.AuthErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validatePasswordConfirm(request.password(), request.passwordConfirm());
        validateDuplicateLoginId(request.loginId());

        User user = User.builder()
                .name(request.name())
                .gender(request.gender())
                .phone(request.phone())
                .build();

        User savedUser = userRepository.save(user);

        LocalCredential localCredential = LocalCredential.builder()
                .user(savedUser)
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .passwordChangedAt(LocalDateTime.now())
                .build();

        localCredentialRepository.save(localCredential);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getOnboardingCompleted()
        );
    }

    public LoginIdCheckResponse checkLoginId(String loginId) {
        boolean available = !localCredentialRepository.existsByLoginId(loginId);
        return new LoginIdCheckResponse(available);
    }

    private void validatePasswordConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new AuthException(AuthErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }
    }

    private void validateDuplicateLoginId(String loginId) {
        if (localCredentialRepository.existsByLoginId(loginId)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        LocalCredential localCredential = localCredentialRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), localCredential.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.LOGIN_FAILED);
        }

        User user = localCredential.getUser();
        AuthMember authMember = new AuthMember(user);

        String accessToken = jwtUtil.createAccessToken(authMember);
        String refreshToken = jwtUtil.createRefreshToken(authMember);

        RefreshToken savedRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plus(jwtUtil.getRefreshTokenExpiration(), ChronoUnit.MILLIS))
                .build();

        refreshTokenRepository.save(savedRefreshToken);

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
}