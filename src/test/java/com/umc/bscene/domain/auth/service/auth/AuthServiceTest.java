package com.umc.bscene.domain.auth.service.auth;

import com.umc.bscene.domain.auth.dto.auth.request.LoginRequest;
import com.umc.bscene.domain.auth.dto.auth.request.LogoutRequest;
import com.umc.bscene.domain.auth.dto.auth.request.ReissueRequest;
import com.umc.bscene.domain.auth.dto.auth.response.ReissueResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
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
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userRepository,
                localCredentialRepository,
                passwordEncoder,
                stringRedisTemplate,
                jwtUtil
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

    @ParameterizedTest
    @CsvSource({
            "SUSPENDED, SUSPENDED_ACCOUNT",
            "INACTIVE, INACTIVE_ACCOUNT",
            "DELETED, DELETED_ACCOUNT"
    })
    void reissue_이용_불가_상태의_계정이면_예외를_던지고_리프레시_토큰을_폐기한다(
            UserStatus status,
            AuthErrorCode expectedCode
    ) {
        ReissueRequest request = new ReissueRequest("old-refresh-token");
        User user = user(100L, status);

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

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.reissue(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(expectedCode);
        verify(stringRedisTemplate).delete(startsWith("refreshToken:"));
        verify(jwtUtil, never()).createAccessToken(any());
        verify(jwtUtil, never()).createRefreshToken(any());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
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

}
