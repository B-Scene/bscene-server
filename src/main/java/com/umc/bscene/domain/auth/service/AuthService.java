package com.umc.bscene.domain.auth.service;

import com.umc.bscene.domain.auth.dto.request.*;
import com.umc.bscene.domain.auth.dto.response.*;
import com.umc.bscene.domain.auth.entity.LocalCredential;
import com.umc.bscene.domain.auth.exception.AuthException;
import com.umc.bscene.domain.auth.phoneverification.enums.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.phoneverification.service.PhoneVerificationService;
import com.umc.bscene.domain.auth.repository.LocalCredentialRepository;
import com.umc.bscene.domain.auth.response.code.AuthErrorCode;
import com.umc.bscene.domain.auth.term.entity.UserTerms;
import com.umc.bscene.domain.auth.term.repository.UserTermsRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    private final PhoneVerificationService phoneVerificationService;

    private final UserTermsRepository userTermsRepository;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        phoneVerificationService.validateVerified(PhoneVerificationPurpose.SIGNUP, request.phone());

        validatePasswordConfirm(request.password(), request.passwordConfirm());
        validateDuplicateLoginId(request.loginId());
        validateDuplicatePhone(request.phone());

        Gender gender = convertGender(request.genderCode());
        LocalDate birthDate = convertBirthDate(
                request.birthDatePrefix(),
                request.genderCode()
        );

        User user = User.builder()
                .name(request.name())
                .gender(gender)
                .birthDate(birthDate)
                .phone(request.phone())
                .build();

        User savedUser = userRepository.save(user);

        LocalCredential localCredential = LocalCredential.builder()
                .user(savedUser)
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .passwordChangedAt(LocalDateTime.now())
                .build();

        try {
            localCredentialRepository.save(localCredential);
        } catch (DataIntegrityViolationException e) {
            throw new AuthException(AuthErrorCode.DUPLICATE_LOGIN_ID);
        }

        saveUserTerms(savedUser, request.termAgreements());

        phoneVerificationService.removeVerified(PhoneVerificationPurpose.SIGNUP, request.phone());

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getOnboardingCompleted()
        );
    }

    private void validateDuplicatePhone(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_PHONE);
        }
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

    private Gender convertGender(String genderCode) {
        return switch (genderCode) {
            case "1", "3" -> Gender.MALE;
            case "2", "4" -> Gender.FEMALE;
            default -> throw new AuthException(AuthErrorCode.INVALID_SIGNUP_REQUEST);
        };
    }

    private LocalDate convertBirthDate(String birthDatePrefix, String genderCode) {
        int yearPrefix = switch (genderCode) {
            case "1", "2" -> 1900;
            case "3", "4" -> 2000;
            default -> throw new AuthException(AuthErrorCode.INVALID_SIGNUP_REQUEST);
        };

        int year = yearPrefix + Integer.parseInt(birthDatePrefix.substring(0, 2));
        int month = Integer.parseInt(birthDatePrefix.substring(2, 4));
        int day = Integer.parseInt(birthDatePrefix.substring(4, 6));

        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new AuthException(AuthErrorCode.INVALID_SIGNUP_REQUEST);
        }
    }

    private void saveUserTerms(User user, List<TermAgreementRequest> termAgreements) {
        List<UserTerms> userTerms = termAgreements.stream()
                .map(agreement -> UserTerms.builder()
                        .user(user)
                        .termId(agreement.termId())
                        .isAgreed(agreement.agreed())
                        .agreedAt(LocalDateTime.now())
                        .build())
                .toList();

        userTermsRepository.saveAll(userTerms);
    }

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

    public LoginIdFindResponse findLoginId(String name, String phone) {
        phoneVerificationService.validateVerified(PhoneVerificationPurpose.FIND_LOGIN_ID, phone);

        LocalCredential localCredential = localCredentialRepository.findByUser_NameAndUser_Phone(name, phone)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        phoneVerificationService.removeVerified(PhoneVerificationPurpose.FIND_LOGIN_ID, phone);

        return LoginIdFindResponse.builder()
                .loginId(maskLoginId(localCredential.getLoginId()))
                .build();
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

    private String maskLoginId(String loginId) {
        int atIndex = loginId.indexOf("@");

        if (atIndex <= 0) {
            return "****";
        }

        String localPart = loginId.substring(0, atIndex);
        String domainPart = loginId.substring(atIndex);

        if (localPart.length() == 1) {
            return localPart + domainPart;
        }

        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "*".repeat(localPart.length() - 1) + domainPart;
        }

        String visiblePart = localPart.substring(0, localPart.length() - 3);

        return visiblePart + "***" + domainPart;
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

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        phoneVerificationService.validateVerified(PhoneVerificationPurpose.PASSWORD_RESET, request.phone());
        validatePasswordConfirm(request.newPassword(), request.newPasswordConfirm());

        LocalCredential localCredential = localCredentialRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        if (!localCredential.getUser().getPhone().equals(request.phone())) {
            throw new AuthException(AuthErrorCode.MEMBER_NOT_FOUND);
        }

        localCredential.changePassword(passwordEncoder.encode(request.newPassword()));

        phoneVerificationService.removeVerified(PhoneVerificationPurpose.PASSWORD_RESET, request.phone());
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