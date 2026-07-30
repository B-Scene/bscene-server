package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.auth.dto.auth.request.TermAgreementRequest;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.entity.term.UserTerms;
import com.umc.bscene.domain.auth.enums.verification.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.oauth.dto.request.OauthSignupRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 소셜 로그인 (성공 처리 / 일회성 교환 코드 / 소셜 회원가입) 단위테스트.
@ExtendWith(MockitoExtension.class)
class OauthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OauthAccountRepository oauthAccountRepository;
    @Mock
    private UserTermsRepository userTermsRepository;
    @Mock
    private PhoneVerificationService phoneVerificationService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private OAuthResponse oauthInfo;

    private OauthService service;

    private static final Long USER_ID = 1L;
    private static final String EXCHANGE_KEY_PREFIX = "oauth:exchange:";

    @BeforeEach
    void setUp() {
        service = new OauthService(
                userRepository, oauthAccountRepository, userTermsRepository,
                phoneVerificationService, stringRedisTemplate, jwtUtil
        );
    }

    private void mockTokenIssue() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.createAccessToken(any(AuthMember.class))).thenReturn("access-token");
        when(jwtUtil.createRefreshToken(any(AuthMember.class))).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(1209600000L);
    }

    // ---------- handleLoginSuccess ----------

    @Test
    void handleLoginSuccess_신규_유저면_signup_토큰을_발급한다() {
        when(oauthInfo.getProvider()).thenReturn(SocialProvider.KAKAO);
        when(oauthInfo.getProviderUid()).thenReturn("uid-1");
        when(oauthInfo.getName()).thenReturn("세진");
        when(oauthInfo.getEmail()).thenReturn("sejin@kakao.com");
        when(jwtUtil.createSignupToken("KAKAO", "uid-1", "세진", "sejin@kakao.com"))
                .thenReturn("signup-token");

        OauthLoginResponse response = service.handleLoginSuccess(OAuthMember.ofNew(oauthInfo, Map.of()));

        assertTrue(response.isNewUser());
        assertEquals("signup-token", response.signupToken());
        assertEquals("sejin@kakao.com", response.email());
    }

    @Test
    void handleLoginSuccess_기존_유저면_토큰을_발급하고_리프레시_토큰을_저장한다() {
        User user = StreamFixtures.fanUser(USER_ID);
        mockTokenIssue();

        OauthLoginResponse response =
                service.handleLoginSuccess(OAuthMember.ofExisting(user, oauthInfo, Map.of()));

        assertEquals(false, response.isNewUser());
        assertEquals("access-token", response.token().accessToken());
        assertEquals("refresh-token", response.token().refreshToken());
        assertEquals(USER_ID, response.token().user().userId());
        // 리프레시 토큰은 해시 키로 Redis에 저장 (원문 저장 금지)
        verify(valueOperations).set(startsWith("refreshToken:"), eq(String.valueOf(USER_ID)), any(Duration.class));
    }

    // ---------- issueExchangeCode / exchangeCode ----------

    @Test
    void 교환_코드는_저장한_로그인_결과를_그대로_복원한다() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        OauthLoginResponse original = OauthLoginResponse.ofNew("signup-token", "sejin@kakao.com");

        String code = service.issueExchangeCode(original);

        // 저장된 json을 그대로 돌려주는 Redis를 재현해 직렬화 왕복을 검증
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.captor();
        verify(valueOperations).set(eq(EXCHANGE_KEY_PREFIX + code), jsonCaptor.capture(), any(Duration.class));
        when(valueOperations.getAndDelete(EXCHANGE_KEY_PREFIX + code)).thenReturn(jsonCaptor.getValue());

        OauthLoginResponse exchanged = service.exchangeCode(code);

        assertEquals(original, exchanged);
    }

    @Test
    void exchangeCode_없거나_만료된_코드면_예외() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        OauthException exception =
                assertThrows(OauthException.class, () -> service.exchangeCode("만료된코드"));

        assertEquals(OauthErrorCode.INVALID_EXCHANGE_CODE, exception.getBaseResponseCode());
    }

    // ---------- signup ----------

    private OauthSignupRequest signupRequest(String genderCode, String birthDatePrefix) {
        return new OauthSignupRequest(
                "signup-token", "세진", birthDatePrefix, genderCode, "01012345678",
                List.of(new TermAgreementRequest(1L, true)));
    }

    private void mockValidSignupToken() {
        when(jwtUtil.isValid("signup-token")).thenReturn(true);
        when(jwtUtil.getType("signup-token")).thenReturn("signup");
        when(jwtUtil.getProvider("signup-token")).thenReturn("KAKAO");
        when(jwtUtil.getProviderUid("signup-token")).thenReturn("uid-1");
        when(jwtUtil.getEmail("signup-token")).thenReturn("sejin@kakao.com");
    }

    @Test
    void signup_유효하지_않은_토큰이면_예외() {
        when(jwtUtil.isValid("signup-token")).thenReturn(false);

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.INVALID_SIGNUP_TOKEN, exception.getBaseResponseCode());
    }

    @Test
    void signup_signup_타입이_아닌_토큰이면_예외() {
        when(jwtUtil.isValid("signup-token")).thenReturn(true);
        when(jwtUtil.getType("signup-token")).thenReturn("access");

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.INVALID_SIGNUP_TOKEN, exception.getBaseResponseCode());
    }

    @Test
    void signup_지원하지_않는_provider면_예외() {
        when(jwtUtil.isValid("signup-token")).thenReturn(true);
        when(jwtUtil.getType("signup-token")).thenReturn("signup");
        when(jwtUtil.getProvider("signup-token")).thenReturn("NAVER");

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.NOT_SUPPORT_PROVIDER, exception.getBaseResponseCode());
    }

    @Test
    void signup_이미_가입된_소셜_계정이면_예외() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.of(OauthAccount.builder().build()));

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.ALREADY_REGISTERED, exception.getBaseResponseCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_휴대폰_번호가_중복이면_예외() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByPhone("01012345678")).thenReturn(true);

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.DUPLICATE_PHONE, exception.getBaseResponseCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_생년월일이_실존하지_않는_날짜면_예외() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByPhone("01012345678")).thenReturn(false);

        // 13월 40일
        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "021340")));

        assertEquals(OauthErrorCode.INVALID_SIGNUP_INFO, exception.getBaseResponseCode());
    }

    @Test
    void signup_성공시_User와_OauthAccount를_저장하고_토큰을_발급한다() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByPhone("01012345678")).thenReturn(false);
        User savedUser = StreamFixtures.user(USER_ID, null);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        mockTokenIssue();

        // genderCode 3 = 2000년대생 남성, 000101 → 2000-01-01
        TokenResponse response = service.signup(signupRequest("3", "000101"));

        assertEquals("access-token", response.accessToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.captor();
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Gender.MALE, userCaptor.getValue().getGender());
        assertEquals(LocalDate.of(2000, 1, 1), userCaptor.getValue().getBirthDate());

        ArgumentCaptor<OauthAccount> accountCaptor = ArgumentCaptor.captor();
        verify(oauthAccountRepository).save(accountCaptor.capture());
        assertEquals(SocialProvider.KAKAO, accountCaptor.getValue().getProvider());
        assertEquals("uid-1", accountCaptor.getValue().getProviderUid());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserTerms>> termsCaptor = ArgumentCaptor.captor();
        verify(userTermsRepository).saveAll(termsCaptor.capture());
        assertEquals(1, termsCaptor.getValue().size());

        // 인증 완료 상태 재사용 방지까지 수행
        verify(phoneVerificationService).validateVerified(PhoneVerificationPurpose.SIGNUP, "01012345678");
        verify(phoneVerificationService).removeVerified(PhoneVerificationPurpose.SIGNUP, "01012345678");
    }
}
