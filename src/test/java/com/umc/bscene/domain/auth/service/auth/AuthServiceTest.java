package com.umc.bscene.domain.auth.service.auth;

import com.umc.bscene.domain.auth.dto.auth.request.SignupRequest;
import com.umc.bscene.domain.auth.dto.auth.request.TermAgreementRequest;
import com.umc.bscene.domain.auth.dto.auth.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.dto.auth.response.SignupResponse;
import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.entity.term.UserTerms;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.enums.verification.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
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

    @Test
    void signup_성별_코드가_올바르지_않으면_예외() {
        SignupRequest request = signupRequest(
                "test@example.com",
                "Password1!",
                "990101",
                "5",
                "01012345678"
        );

        when(localCredentialRepository.existsByLoginId(request.loginId()))
                .thenReturn(false);
        when(userRepository.existsByPhone(request.phone()))
                .thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.signup(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_SIGNUP_REQUEST);

        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
        verify(userRepository, never()).save(any());
        verify(localCredentialRepository, never()).save(any());
        verify(userTermsRepository, never()).saveAll(any());
    }

    @Test
    void signup_존재하지_않는_생년월일이면_예외() {
        SignupRequest request = signupRequest(
                "test@example.com",
                "Password1!",
                "991332",
                "1",
                "01012345678"
        );

        when(localCredentialRepository.existsByLoginId(request.loginId()))
                .thenReturn(false);
        when(userRepository.existsByPhone(request.phone()))
                .thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.signup(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_SIGNUP_REQUEST);

        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
        verify(userRepository, never()).save(any());
        verify(localCredentialRepository, never()).save(any());
        verify(userTermsRepository, never()).saveAll(any());
    }

    @ParameterizedTest(name = "성별 코드 {1}, 생년월일 {0}")
    @CsvSource({
            "990101, 1, MALE, 1999-01-01",
            "990101, 2, FEMALE, 1999-01-01",
            "010101, 3, MALE, 2001-01-01",
            "010101, 4, FEMALE, 2001-01-01"
    })
    void signup_유효한_요청이면_회원가입_정보를_저장한다(
            String birthDatePrefix,
            String genderCode,
            Gender expectedGender,
            String expectedBirthDate
    ) {
        SignupRequest request = signupRequest(
                "test@example.com",
                "Password1!",
                birthDatePrefix,
                genderCode,
                "01012345678"
        );
        User savedUser = User.builder()
                .id(100L)
                .name(request.name())
                .birthDate(LocalDate.parse(expectedBirthDate))
                .gender(expectedGender)
                .phone(request.phone())
                .build();

        when(localCredentialRepository.existsByLoginId(request.loginId()))
                .thenReturn(false);
        when(userRepository.existsByPhone(request.phone()))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");

        SignupResponse response = service.signup(request);

        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.onboardingCompleted()).isFalse();

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getName()).isEqualTo("테스트유저");
        assertThat(createdUser.getBirthDate())
                .isEqualTo(LocalDate.parse(expectedBirthDate));
        assertThat(createdUser.getGender()).isEqualTo(expectedGender);
        assertThat(createdUser.getPhone()).isEqualTo(request.phone());

        ArgumentCaptor<LocalCredential> credentialCaptor =
                ArgumentCaptor.forClass(LocalCredential.class);
        verify(localCredentialRepository).save(credentialCaptor.capture());

        LocalCredential createdCredential = credentialCaptor.getValue();
        assertThat(createdCredential.getUser()).isSameAs(savedUser);
        assertThat(createdCredential.getLoginId())
                .isEqualTo(request.loginId());
        assertThat(createdCredential.getPasswordHash())
                .isEqualTo("encoded-password");
        assertThat(createdCredential.getPasswordChangedAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserTerms>> termsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(userTermsRepository).saveAll(termsCaptor.capture());

        List<UserTerms> savedTerms = termsCaptor.getValue();
        assertThat(savedTerms).hasSize(2);
        assertThat(savedTerms.get(0).getUser()).isSameAs(savedUser);
        assertThat(savedTerms.get(0).getTermId()).isEqualTo(1L);
        assertThat(savedTerms.get(0).getIsAgreed()).isTrue();
        assertThat(savedTerms.get(0).getAgreedAt()).isNotNull();
        assertThat(savedTerms.get(1).getUser()).isSameAs(savedUser);
        assertThat(savedTerms.get(1).getTermId()).isEqualTo(2L);
        assertThat(savedTerms.get(1).getIsAgreed()).isFalse();
        assertThat(savedTerms.get(1).getAgreedAt()).isNotNull();

        verify(phoneVerificationService).removeVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
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
                "테스트유저",
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
