package com.umc.bscene.domain.auth.service.auth;

import com.umc.bscene.domain.auth.dto.auth.request.SignupRequest;
import com.umc.bscene.domain.auth.dto.auth.request.TermAgreementRequest;
import com.umc.bscene.domain.auth.dto.auth.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.auth.request.LogoutRequest;
import com.umc.bscene.domain.auth.dto.auth.request.PasswordResetRequest;
import com.umc.bscene.domain.auth.dto.auth.request.ReissueRequest;
import com.umc.bscene.domain.auth.dto.auth.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.dto.auth.response.LoginIdFindResponse;
import com.umc.bscene.domain.auth.dto.auth.response.ReissueResponse;
import com.umc.bscene.domain.auth.dto.auth.response.SignupResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.entity.term.Terms;
import com.umc.bscene.domain.auth.entity.term.UserTerms;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.enums.verification.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.auth.repository.term.TermsRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.enums.UserStatus;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private UserTermsRepository userTermsRepository;

    @Mock
    private TermsRepository termsRepository;

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
                userTermsRepository,
                termsRepository
        );
        lenient().when(termsRepository.getReferenceById(anyLong()))
                .thenAnswer(invocation -> Terms.builder()
                        .termId(invocation.getArgument(0))
                        .build());
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
        assertThat(savedTerms.get(0).getTerms().getTermId()).isEqualTo(1L);
        assertThat(savedTerms.get(0).getIsAgreed()).isTrue();
        assertThat(savedTerms.get(0).getAgreedAt()).isNotNull();
        assertThat(savedTerms.get(1).getUser()).isSameAs(savedUser);
        assertThat(savedTerms.get(1).getTerms().getTermId()).isEqualTo(2L);
        assertThat(savedTerms.get(1).getIsAgreed()).isFalse();
        assertThat(savedTerms.get(1).getAgreedAt()).isNotNull();

        verify(phoneVerificationService).removeVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
    }

    @Test
    void signup_자격증명_저장_중_아이디가_충돌하면_중복_아이디_예외() {
        SignupRequest request = signupRequest();
        User savedUser = User.builder()
                .id(100L)
                .name(request.name())
                .birthDate(LocalDate.of(1999, 1, 1))
                .gender(Gender.MALE)
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
        when(localCredentialRepository.save(any(LocalCredential.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate loginId"
                ));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.signup(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.DUPLICATE_LOGIN_ID);

        verify(localCredentialRepository)
                .save(any(LocalCredential.class));
        verify(userTermsRepository, never()).saveAll(any());
        verify(phoneVerificationService, never()).removeVerified(
                PhoneVerificationPurpose.SIGNUP,
                request.phone()
        );
    }

    // ---------- login ----------

    @Test
    void login_아이디가_존재하지_않으면_로그인_실패() {
        LoginRequest request =
                new LoginRequest("missing@example.com", "Password1!");

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.login(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.LOGIN_FAILED);
        verifyNoInteractions(passwordEncoder, jwtUtil, stringRedisTemplate);
    }

    @Test
    void login_비밀번호가_일치하지_않으면_로그인_실패() {
        LoginRequest request =
                new LoginRequest("test@example.com", "WrongPassword1!");
        LocalCredential credential = localCredential(
                activeUser(),
                request.loginId(),
                "encoded-password"
        );

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(
                request.password(),
                credential.getPasswordHash()
        )).thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.login(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.LOGIN_FAILED);
        verifyNoInteractions(jwtUtil, stringRedisTemplate);
    }

    @ParameterizedTest(name = "{0} 계정 로그인 차단")
    @CsvSource({
            "SUSPENDED, SUSPENDED_ACCOUNT",
            "INACTIVE, INACTIVE_ACCOUNT",
            "DELETED, DELETED_ACCOUNT"
    })
    void login_이용할_수_없는_계정이면_상태에_맞는_예외(
            UserStatus status,
            AuthErrorCode expectedErrorCode
    ) {
        LoginRequest request =
                new LoginRequest("test@example.com", "Password1!");
        LocalCredential credential = localCredential(
                user(100L, status),
                request.loginId(),
                "encoded-password"
        );

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(
                request.password(),
                credential.getPasswordHash()
        )).thenReturn(true);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.login(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(expectedErrorCode);
        verifyNoInteractions(jwtUtil, stringRedisTemplate);
    }

    @Test
    void login_유효한_계정이면_토큰을_발급하고_리프레시_토큰을_저장한다() {
        LoginRequest request =
                new LoginRequest("test@example.com", "Password1!");
        User user = activeUser();
        LocalCredential credential = localCredential(
                user,
                request.loginId(),
                "encoded-password"
        );

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(
                request.password(),
                credential.getPasswordHash()
        )).thenReturn(true);
        when(jwtUtil.createAccessToken(any()))
                .thenReturn("access-token");
        when(jwtUtil.createRefreshToken(any()))
                .thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration())
                .thenReturn(3_600_000L);
        when(jwtUtil.getRefreshTokenExpiration())
                .thenReturn(604_800_000L);
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        TokenResponse response = service.login(request);

        assertThat(response.grantType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(3_600_000L);
        assertThat(response.user().userId()).isEqualTo(user.getId());
        assertThat(response.user().name()).isEqualTo(user.getName());
        assertThat(response.user().currentMode())
                .isEqualTo(UserMode.FAN);
        assertThat(response.user().onboardingCompleted()).isTrue();

        ArgumentCaptor<String> redisKeyCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                redisKeyCaptor.capture(),
                eq(String.valueOf(user.getId())),
                eq(Duration.ofMillis(604_800_000L))
        );

        assertThat(redisKeyCaptor.getValue())
                .startsWith("refreshToken:")
                .doesNotContain("refresh-token");
    }

    // ---------- findLoginId ----------

    @Test
    void findLoginId_일치하는_회원이_없으면_예외() {
        String name = "테스트유저";
        String phone = "01012345678";

        when(localCredentialRepository.findByUser_NameAndUser_Phone(
                name,
                phone
        )).thenReturn(Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.findLoginId(name, phone)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND);
        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.FIND_LOGIN_ID,
                phone
        );
        verify(phoneVerificationService, never()).removeVerified(
                PhoneVerificationPurpose.FIND_LOGIN_ID,
                phone
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "plain, ****",
            "a@example.com, a@example.com",
            "ab@example.com, a*@example.com",
            "abc@example.com, a**@example.com",
            "abcdef@example.com, abc***@example.com"
    })
    void findLoginId_일치하는_회원이면_아이디를_마스킹해서_반환한다(
            String loginId,
            String expectedMaskedLoginId
    ) {
        User user = activeUser();
        LocalCredential credential = localCredential(
                user,
                loginId,
                "encoded-password"
        );

        when(localCredentialRepository.findByUser_NameAndUser_Phone(
                user.getName(),
                user.getPhone()
        )).thenReturn(Optional.of(credential));

        LoginIdFindResponse response = service.findLoginId(
                user.getName(),
                user.getPhone()
        );

        assertThat(response.loginId()).isEqualTo(expectedMaskedLoginId);
        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.FIND_LOGIN_ID,
                user.getPhone()
        );
        verify(phoneVerificationService).removeVerified(
                PhoneVerificationPurpose.FIND_LOGIN_ID,
                user.getPhone()
        );
    }

    // ---------- resetPassword ----------

    @Test
    void resetPassword_새_비밀번호_확인이_일치하지_않으면_예외() {
        PasswordResetRequest request = new PasswordResetRequest(
                "test@example.com",
                "01012345678",
                "NewPassword1!",
                "DifferentPassword1!"
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        verify(phoneVerificationService).validateVerified(
                PhoneVerificationPurpose.PASSWORD_RESET,
                request.phone()
        );
        verifyNoInteractions(localCredentialRepository, passwordEncoder);
    }

    @Test
    void resetPassword_로그인_정보가_없으면_예외() {
        PasswordResetRequest request = passwordResetRequest();

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND);
        verify(passwordEncoder, never()).encode(anyString());
        verify(phoneVerificationService, never()).removeVerified(
                PhoneVerificationPurpose.PASSWORD_RESET,
                request.phone()
        );
    }

    @Test
    void resetPassword_가입한_휴대폰_번호와_다르면_예외() {
        PasswordResetRequest request = passwordResetRequest();
        LocalCredential credential = localCredential(
                activeUser(),
                request.loginId(),
                "old-password"
        );

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.of(credential));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.resetPassword(new PasswordResetRequest(
                        request.loginId(),
                        "01099999999",
                        request.newPassword(),
                        request.newPasswordConfirm()
                ))
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND);
        verify(passwordEncoder, never()).encode(anyString());
        verify(phoneVerificationService, never()).removeVerified(
                eq(PhoneVerificationPurpose.PASSWORD_RESET),
                anyString()
        );
    }

    @Test
    void resetPassword_회원정보가_일치하면_비밀번호를_변경한다() {
        PasswordResetRequest request = passwordResetRequest();
        LocalCredential credential = localCredential(
                activeUser(),
                request.loginId(),
                "old-password"
        );

        when(localCredentialRepository.findByLoginId(request.loginId()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.encode(request.newPassword()))
                .thenReturn("new-encoded-password");

        service.resetPassword(request);

        assertThat(credential.getPasswordHash())
                .isEqualTo("new-encoded-password");
        assertThat(credential.getPasswordChangedAt()).isNotNull();
        verify(phoneVerificationService).removeVerified(
                PhoneVerificationPurpose.PASSWORD_RESET,
                request.phone()
        );
    }

    // ---------- reissue ----------

    @Test
    void reissue_유효하지_않은_토큰이면_예외() {
        ReissueRequest request = new ReissueRequest("invalid-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.reissue(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verify(jwtUtil, never()).getType(anyString());
        verifyNoInteractions(stringRedisTemplate, userRepository);
    }

    @Test
    void reissue_리프레시_타입이_아닌_토큰이면_예외() {
        ReissueRequest request = new ReissueRequest("access-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("access");

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.reissue(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(stringRedisTemplate, userRepository);
    }

    @Test
    void reissue_레디스에_저장된_토큰이_없으면_예외() {
        ReissueRequest request = new ReissueRequest("refresh-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("refresh");
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(null);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.reissue(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(userRepository);
    }

    @Test
    void reissue_토큰의_사용자가_존재하지_않으면_예외() {
        ReissueRequest request = new ReissueRequest("refresh-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("refresh");
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn("100");
        when(userRepository.findById(100L))
                .thenReturn(Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.reissue(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void reissue_유효한_리프레시_토큰이면_토큰을_재발급한다() {
        ReissueRequest request = new ReissueRequest("old-refresh-token");
        User user = activeUser();

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("refresh");
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(String.valueOf(user.getId()));
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(jwtUtil.createAccessToken(any()))
                .thenReturn("new-access-token");
        when(jwtUtil.createRefreshToken(any()))
                .thenReturn("new-refresh-token");
        when(jwtUtil.getAccessTokenExpiration())
                .thenReturn(3_600_000L);
        when(jwtUtil.getRefreshTokenExpiration())
                .thenReturn(604_800_000L);

        ReissueResponse response = service.reissue(request);

        assertThat(response.grantType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(3_600_000L);

        ArgumentCaptor<String> deletedKeyCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> savedKeyCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(stringRedisTemplate).delete(deletedKeyCaptor.capture());
        verify(valueOperations).set(
                savedKeyCaptor.capture(),
                eq(String.valueOf(user.getId())),
                eq(Duration.ofMillis(604_800_000L))
        );

        assertThat(deletedKeyCaptor.getValue())
                .startsWith("refreshToken:")
                .doesNotContain(request.refreshToken());
        assertThat(savedKeyCaptor.getValue())
                .startsWith("refreshToken:")
                .doesNotContain("new-refresh-token")
                .isNotEqualTo(deletedKeyCaptor.getValue());
    }

    // ---------- logout ----------

    @Test
    void logout_유효하지_않은_토큰이면_예외() {
        LogoutRequest request = new LogoutRequest("invalid-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.logout(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verify(jwtUtil, never()).getType(anyString());
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void logout_리프레시_타입이_아닌_토큰이면_예외() {
        LogoutRequest request = new LogoutRequest("access-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("access");

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.logout(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void logout_레디스에_저장된_토큰이_없으면_예외() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("refresh");
        when(stringRedisTemplate.delete(anyString()))
                .thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.logout(request)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void logout_유효한_리프레시_토큰이면_레디스에서_삭제한다() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        when(jwtUtil.isValid(request.refreshToken()))
                .thenReturn(true);
        when(jwtUtil.getType(request.refreshToken()))
                .thenReturn("refresh");
        when(stringRedisTemplate.delete(anyString()))
                .thenReturn(true);

        service.logout(request);

        ArgumentCaptor<String> redisKeyCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(stringRedisTemplate).delete(redisKeyCaptor.capture());
        assertThat(redisKeyCaptor.getValue())
                .startsWith("refreshToken:")
                .doesNotContain(request.refreshToken());
    }

    private User activeUser() {
        return user(100L, UserStatus.ACTIVE);
    }

    private User user(Long id, UserStatus status) {
        return User.builder()
                .id(id)
                .name("테스트유저")
                .birthDate(LocalDate.of(1999, 1, 1))
                .gender(Gender.MALE)
                .phone("01012345678")
                .currentMode(UserMode.FAN)
                .onboardingCompleted(true)
                .status(status)
                .build();
    }

    private LocalCredential localCredential(
            User user,
            String loginId,
            String passwordHash
    ) {
        return LocalCredential.builder()
                .id(200L)
                .user(user)
                .loginId(loginId)
                .passwordHash(passwordHash)
                .passwordChangedAt(LocalDateTime.now())
                .build();
    }

    private PasswordResetRequest passwordResetRequest() {
        return new PasswordResetRequest(
                "test@example.com",
                "01012345678",
                "NewPassword1!",
                "NewPassword1!"
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
