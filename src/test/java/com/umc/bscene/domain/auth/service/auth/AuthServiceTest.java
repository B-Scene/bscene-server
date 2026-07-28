package com.umc.bscene.domain.auth.service.auth;

import com.umc.bscene.domain.auth.dto.auth.request.SignupRequest;
import com.umc.bscene.domain.auth.dto.auth.request.TermAgreementRequest;
import com.umc.bscene.domain.auth.dto.auth.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.enums.verification.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalCredentialRepository localCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private UserTermsRepository userTermsRepository;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userRepository,
                localCredentialRepository,
                passwordEncoder,
                stringRedisTemplate,
                jwtUtil,
                phoneVerificationService,
                userTermsRepository
        );
    }

    // ---------- checkLoginId ----------

    @Test
    void checkLoginId_사용하지_않는_아이디면_사용_가능하다() {
        when(localCredentialRepository.existsByLoginId("available@example.com"))
                .thenReturn(false);

        LoginIdCheckResponse response =
                service.checkLoginId("available@example.com");

        assertThat(response.available()).isTrue();

        verify(localCredentialRepository)
                .existsByLoginId("available@example.com");
    }

    @Test
    void checkLoginId_이미_사용_중인_아이디면_사용_불가능하다() {
        when(localCredentialRepository.existsByLoginId("duplicate@example.com"))
                .thenReturn(true);

        LoginIdCheckResponse response =
                service.checkLoginId("duplicate@example.com");

        assertThat(response.available()).isFalse();

        verify(localCredentialRepository)
                .existsByLoginId("duplicate@example.com");
    }

    // ---------- signup ----------

    @Test
    void signup_비밀번호_확인이_일치하지_않으면_예외() {
        SignupRequest request = signupRequest(
                "test@example.com",
                "Different1!",
                "990101",
                "1",
                "01012345678"
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.signup(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.PASSWORD_CONFIRM_NOT_MATCH);

        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
        verifyNoInteractions(
                userRepository,
                localCredentialRepository,
                passwordEncoder,
                userTermsRepository
        );
    }

    @Test
    void signup_이미_사용_중인_로그인_아이디면_예외() {
        SignupRequest request = signupRequest();

        when(localCredentialRepository.existsByLoginId(request.loginId()))
                .thenReturn(true);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.signup(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.DUPLICATE_LOGIN_ID);

        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
        verify(localCredentialRepository)
                .existsByLoginId(request.loginId());
        verify(userRepository, never())
                .existsByPhone(any());
        verify(userRepository, never())
                .save(any());
    }

    @Test
    void signup_이미_가입된_휴대폰_번호면_예외() {
        SignupRequest request = signupRequest();

        when(localCredentialRepository.existsByLoginId(request.loginId()))
                .thenReturn(false);
        when(userRepository.existsByPhone(request.phone()))
                .thenReturn(true);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.signup(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.DUPLICATE_PHONE);

        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
        verify(localCredentialRepository)
                .existsByLoginId(request.loginId());
        verify(userRepository)
                .existsByPhone(request.phone());
        verify(userRepository, never())
                .save(any());
        verify(localCredentialRepository, never())
                .save(any());
    }

    private SignupRequest signupRequest() {
        return signupRequest(
                "test@example.com",
                "Password1!",
                "990101",
                "1",
                "01012345678"
        );
    }

    private SignupRequest signupRequest(
            String loginId,
            String passwordConfirm,
            String birthDatePrefix,
            String genderCode,
            String phone
    ) {
        return new SignupRequest(
                loginId,
                "Password1!",
                passwordConfirm,
                "고태영",
                birthDatePrefix,
                genderCode,
                phone,
                List.of(
                        new TermAgreementRequest(1L, true),
                        new TermAgreementRequest(2L, false)
                )
        );
    }
}