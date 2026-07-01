package com.umc.bscene.domain.auth.service;

import com.umc.bscene.domain.auth.dto.request.SignupRequest;
import com.umc.bscene.domain.auth.dto.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.dto.response.SignupResponse;
import com.umc.bscene.domain.auth.entity.LocalCredential;
import com.umc.bscene.domain.auth.exception.AuthException;
import com.umc.bscene.domain.auth.repository.LocalCredentialRepository;
import com.umc.bscene.domain.auth.response.code.AuthErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

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
}