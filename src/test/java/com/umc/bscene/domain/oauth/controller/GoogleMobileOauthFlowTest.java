package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.auth.repository.term.TermsRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.security.oauth.google.GoogleIdTokenVerifier;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.GlobalExceptionHandler;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GoogleMobileOauthFlowTest {

    private static final String ID_TOKEN = "test-google-id-token";
    private static final String GOOGLE_SUB = "google-sub";
    private static final String PATH = "/auth/oauth/mobile/google";

    @Mock
    private GoogleIdTokenVerifier verifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OauthAccountRepository oauthAccountRepository;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private JwtUtil jwtUtil;

    private OauthService oauthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        oauthService = new OauthService(
                userRepository, oauthAccountRepository, mock(UserTermsRepository.class), mock(TermsRepository.class),
                mock(PhoneVerificationService.class), redis, jwtUtil
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new GoogleMobileOauthController(verifier, oauthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void connectExistingGoogleMemberToSameUserOnWebAndMobile() throws Exception {
        User user = StreamFixtures.fanUser(42L);
        OauthAccount account = createExistingGoogleAccount(user);
        mockVerifiedGoogleIdentity(null, null);
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.GOOGLE, GOOGLE_SUB))
                .thenReturn(Optional.of(account));
        mockTokenIssuance();

        OauthLoginResponse webResponse = oauthService.handleLoginSuccess(
                OAuthMember.ofExisting(user, mock(OAuthResponse.class), Map.of())
        );
        performLogin()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isNewUser").value(false))
                .andExpect(jsonPath("$.result.token.user.userId").value(webResponse.token().user().userId()));

        assertThat(webResponse.token().user().userId()).isEqualTo(42L);
        assertExistingMemberReuse(account);
    }

    @Test
    void issueCommonSignupTokenForNewGoogleMember() throws Exception {
        mockNewGoogleMember("new@example.com", "테스트 사용자");
        when(jwtUtil.createSignupToken("GOOGLE", GOOGLE_SUB, "테스트 사용자", "new@example.com"))
                .thenReturn("signup-token");

        performLogin()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.signupToken").value("signup-token"))
                .andExpect(jsonPath("$.result.email").value("new@example.com"));

        verifyNoServiceTokensOrMemberCreation();
    }

    @Test
    void rejectNewGoogleMemberWithoutEmailBeforeTokenIssuance() throws Exception {
        mockNewGoogleMember(null, null);

        performLogin()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OAUTH400_4"));

        verifyNoInteractions(jwtUtil, redis, userRepository);
    }

    private OauthAccount createExistingGoogleAccount(User user) {
        return OauthAccount.builder()
                .user(user)
                .provider(SocialProvider.GOOGLE)
                .providerUid(GOOGLE_SUB)
                .email("stored@example.com")
                .build();
    }

    private void mockVerifiedGoogleIdentity(String email, String name) {
        when(verifier.verify(ID_TOKEN))
                .thenReturn(new VerifiedSocialIdentity(SocialProvider.GOOGLE, GOOGLE_SUB, email, name));
    }

    private void mockNewGoogleMember(String email, String name) {
        mockVerifiedGoogleIdentity(email, name);
        when(oauthAccountRepository.findByProviderAndProviderUid(SocialProvider.GOOGLE, GOOGLE_SUB))
                .thenReturn(Optional.empty());
    }

    private ResultActions performLogin() throws Exception {
        return mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + ID_TOKEN + "\"}"));
    }

    private void assertExistingMemberReuse(OauthAccount account) {
        assertThat(account.getEmail()).isEqualTo("stored@example.com");
        verify(oauthAccountRepository).findByProviderAndProviderUid(SocialProvider.GOOGLE, GOOGLE_SUB);
        verify(oauthAccountRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).createSignupToken(anyString(), anyString(), any(), any());
        verify(valueOperations, times(2)).set(startsWith("refreshToken:"), eq("42"), any(Duration.class));
    }

    private void verifyNoServiceTokensOrMemberCreation() {
        verify(jwtUtil, never()).createAccessToken(any());
        verify(jwtUtil, never()).createRefreshToken(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(redis);
    }

    private void mockTokenIssuance() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.createAccessToken(any(AuthMember.class))).thenReturn("bscene-access");
        when(jwtUtil.createRefreshToken(any(AuthMember.class))).thenReturn("bscene-refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(1209600000L);
    }
}
