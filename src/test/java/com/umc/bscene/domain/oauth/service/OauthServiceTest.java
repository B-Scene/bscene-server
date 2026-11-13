package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.auth.dto.auth.request.TermAgreementRequest;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.entity.term.Terms;
import com.umc.bscene.domain.auth.entity.term.UserTerms;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.auth.repository.term.TermsRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.oauth.dto.request.OauthSignupRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.ObjectProvider;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private TermsRepository termsRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private OAuthResponse oauthInfo;
    @Mock
    private ObjectProvider<AppleCredentialService> appleCredentials;
    @Mock
    private AppleCredentialService appleCredentialService;

    private OauthService service;

    private static final Long USER_ID = 1L;
    private static final String EXCHANGE_KEY_PREFIX = "oauth:exchange:";

    @BeforeEach
    void setUp() {
        service = new OauthService(
                userRepository, oauthAccountRepository, userTermsRepository, termsRepository,
                stringRedisTemplate, jwtUtil, appleCredentials
        );
        lenient().when(termsRepository.getReferenceById(anyLong()))
                .thenAnswer(invocation -> Terms.builder()
                        .termId(invocation.getArgument(0))
                        .build());
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
        when(oauthInfo.getProvider()).thenReturn(SocialProvider.KAKAO);
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

    @Test
    void handleLoginSuccess_기존_유저가_탈퇴_상태면_토큰을_발급하지_않는다() {
        // 웹 경로에도 계정 상태 검사가 적용되는지 확인
        User deleted = User.builder().id(USER_ID).name("세진").status(UserStatus.DELETED).build();
        when(oauthInfo.getProvider()).thenReturn(SocialProvider.KAKAO);

        AuthException e = assertThrows(AuthException.class,
                () -> service.handleLoginSuccess(OAuthMember.ofExisting(deleted, oauthInfo, Map.of())));

        assertEquals(AuthErrorCode.DELETED_ACCOUNT, e.getBaseResponseCode());
        verify(jwtUtil, never()).createAccessToken(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void handleLoginSuccess_Google_웹_신원은_토큰을_발급하지_않는다(boolean existing) {
        when(oauthInfo.getProvider()).thenReturn(SocialProvider.GOOGLE);
        OAuthMember member = existing
                ? OAuthMember.ofExisting(StreamFixtures.fanUser(USER_ID), oauthInfo, Map.of())
                : OAuthMember.ofNew(oauthInfo, Map.of());

        OauthException exception = assertThrows(OauthException.class, () -> service.handleLoginSuccess(member));

        assertEquals(OauthErrorCode.NOT_SUPPORT_PROVIDER, exception.getBaseResponseCode());
        verifyNoInteractions(jwtUtil, stringRedisTemplate, oauthAccountRepository, userRepository);
    }

    // ---------- loginWithVerifiedIdentity (모바일 SDK 공통 입구) ----------

    private static final VerifiedSocialIdentity KAKAO_IDENTITY =
            new VerifiedSocialIdentity(SocialProvider.KAKAO, "uid-1", "sejin@kakao.com", "세진");

    private OauthAccount kakaoAccountOf(User user) {
        return OauthAccount.builder()
                .user(user)
                .provider(SocialProvider.KAKAO)
                .providerUid("uid-1")
                .email("sejin@kakao.com")
                .build();
    }

    @Test
    void loginWithVerifiedIdentity_기존_회원이면_토큰을_발급한다() {
        User user = StreamFixtures.fanUser(USER_ID);
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.of(kakaoAccountOf(user)));
        mockTokenIssue();

        OauthLoginResponse response = service.loginWithVerifiedIdentity(KAKAO_IDENTITY);

        assertEquals(false, response.isNewUser());
        assertEquals("access-token", response.token().accessToken());
        assertEquals(USER_ID, response.token().user().userId());
        verify(jwtUtil, never()).createSignupToken(anyString(), anyString(), any(), any());
    }

    @Test
    void loginWithVerifiedIdentity_미가입이면_signup_토큰을_발급한다() {
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        when(jwtUtil.createSignupToken("KAKAO", "uid-1", "세진", "sejin@kakao.com"))
                .thenReturn("signup-token");

        OauthLoginResponse response = service.loginWithVerifiedIdentity(KAKAO_IDENTITY);

        assertTrue(response.isNewUser());
        assertEquals("signup-token", response.signupToken());
        assertEquals("sejin@kakao.com", response.email());
        assertEquals(null, response.token());
    }

    @Test
    void loginWithVerifiedIdentity_미가입인데_이메일이_없으면_예외() {
        // 명세 v0.3: 신규 가입은 이메일 필수(OAUTH400_4), signupToken 미발급
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        VerifiedSocialIdentity noEmail = new VerifiedSocialIdentity(SocialProvider.KAKAO, "uid-1", " ", "세진");

        OauthException e = assertThrows(OauthException.class, () -> service.loginWithVerifiedIdentity(noEmail));

        assertEquals(OauthErrorCode.EMAIL_NOT_PROVIDED, e.getBaseResponseCode());
        verify(jwtUtil, never()).createSignupToken(anyString(), anyString(), any(), any());
    }

    @Test
    void loginWithVerifiedIdentity_정지_계정이면_토큰을_발급하지_않는다() {
        User suspended = User.builder().id(USER_ID).name("세진").status(UserStatus.SUSPENDED).build();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.of(kakaoAccountOf(suspended)));

        AuthException e = assertThrows(AuthException.class, () -> service.loginWithVerifiedIdentity(KAKAO_IDENTITY));

        assertEquals(AuthErrorCode.SUSPENDED_ACCOUNT, e.getBaseResponseCode());
        verify(jwtUtil, never()).createAccessToken(any());
    }

    @Test
    void loginWithVerifiedIdentity_Google_신원은_회원_조회_전에_거절한다() {
        VerifiedSocialIdentity identity =
                new VerifiedSocialIdentity(SocialProvider.GOOGLE, "google-sub", "member@example.com", null);

        OauthException exception = assertThrows(OauthException.class,
                () -> service.loginWithVerifiedIdentity(identity));

        assertEquals(OauthErrorCode.NOT_SUPPORT_PROVIDER, exception.getBaseResponseCode());
        verifyNoInteractions(oauthAccountRepository, userRepository, jwtUtil, stringRedisTemplate);
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
        mockValidSignupToken(SocialProvider.KAKAO);
    }

    private void mockValidSignupToken(SocialProvider provider) {
        when(jwtUtil.isValid("signup-token")).thenReturn(true);
        when(jwtUtil.getType("signup-token")).thenReturn("signup");
        when(jwtUtil.getProvider("signup-token")).thenReturn(provider.name());
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
    void signup_기발급_Google_토큰은_인증과_회원_저장_전에_거절한다() {
        when(jwtUtil.isValid("signup-token")).thenReturn(true);
        when(jwtUtil.getType("signup-token")).thenReturn("signup");
        when(jwtUtil.getProvider("signup-token")).thenReturn("GOOGLE");

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.NOT_SUPPORT_PROVIDER, exception.getBaseResponseCode());
        verifyNoInteractions(userRepository, oauthAccountRepository, userTermsRepository, termsRepository,
                stringRedisTemplate);
        verify(jwtUtil, never()).createAccessToken(any());
        verify(jwtUtil, never()).createRefreshToken(any());
    }

    @Test
    void signup_이미_가입된_소셜_계정이면_예외() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.of(OauthAccount.builder().build()));

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.ALREADY_REGISTERED, exception.getBaseResponseCode());
        verify(userRepository, never()).saveAndFlush(any());
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
        verify(userRepository, never()).saveAndFlush(any());
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

    @ParameterizedTest
    @EnumSource(value = SocialProvider.class, names = {"KAKAO", "APPLE"})
    void signup_문자인증없이_User와_OauthAccount를_저장하고_토큰을_발급한다(SocialProvider provider) {
        mockValidSignupToken(provider);
        if (provider == SocialProvider.APPLE) {
            when(appleCredentials.getObject()).thenReturn(appleCredentialService);
        }
        when(oauthAccountRepository.findByProviderAndProviderUid(provider, "uid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByPhone("01012345678")).thenReturn(false);
        User savedUser = StreamFixtures.user(USER_ID, null);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        mockTokenIssue();

        // genderCode 3 = 2000년대생 남성, 000101 → 2000-01-01
        TokenResponse response = service.signup(signupRequest("3", "000101"));

        assertEquals("access-token", response.accessToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.captor();
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(Gender.MALE, userCaptor.getValue().getGender());
        assertEquals(LocalDate.of(2000, 1, 1), userCaptor.getValue().getBirthDate());

        ArgumentCaptor<OauthAccount> accountCaptor = ArgumentCaptor.captor();
        verify(oauthAccountRepository).saveAndFlush(accountCaptor.capture());
        assertEquals(provider, accountCaptor.getValue().getProvider());
        assertEquals("uid-1", accountCaptor.getValue().getProviderUid());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserTerms>> termsCaptor = ArgumentCaptor.captor();
        verify(userTermsRepository).saveAll(termsCaptor.capture());
        assertEquals(1, termsCaptor.getValue().size());

        if (provider == SocialProvider.APPLE) {
            verify(appleCredentialService).validateSignup("signup-token");
            verify(appleCredentialService).attachSignup("signup-token", savedUser);
        }
    }

    // ---------- signup 동시 요청 (명세 v0.3: 사전 검사는 통과했지만 insert 에서 unique 위반) ----------

    @Test
    void signup_사전검사_통과_후_같은_번호_가입이_먼저_끝나면_OAUTH409_2() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByPhone("01012345678")).thenReturn(false);
        // existsByPhone 과 insert 사이에 다른 요청이 같은 번호로 가입 완료 → user.phone unique 위반
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '01012345678' for key 'user.phone'"));

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.DUPLICATE_PHONE, exception.getBaseResponseCode());
        // User 가 안 만들어졌으니 소셜 계정·약관 저장은 일어나지 않는다
        verify(oauthAccountRepository, never()).saveAndFlush(any());
        verify(userTermsRepository, never()).saveAll(any());
    }

    @Test
    void signup_사전검사_통과_후_같은_소셜_계정_가입이_먼저_끝나면_OAUTH409_1() {
        mockValidSignupToken();
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "uid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByPhone("01012345678")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(StreamFixtures.user(USER_ID, null));
        // findByProviderAndProviderUid 와 insert 사이에 같은 소셜 계정으로 가입 완료 → (provider, provider_uid) unique 위반 (V56)
        when(oauthAccountRepository.saveAndFlush(any(OauthAccount.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'KAKAO-uid-1' for key 'uk_oauth_accounts_provider_uid'"));

        OauthException exception =
                assertThrows(OauthException.class, () -> service.signup(signupRequest("3", "000101")));

        assertEquals(OauthErrorCode.ALREADY_REGISTERED, exception.getBaseResponseCode());
        // 트랜잭션 롤백으로 User 도 남지 않는다. 여기서는 이후 단계가 진행되지 않았음만 확인
        verify(userTermsRepository, never()).saveAll(any());
    }
}
