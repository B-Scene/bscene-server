package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.repository.term.TermsRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.oauth.dto.request.AppleMobileLoginRequest;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.service.AppleChallengeService;
import com.umc.bscene.domain.oauth.service.AppleLoginCompletion;
import com.umc.bscene.domain.oauth.service.AppleMobileOauthService;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.GlobalExceptionHandler;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.oauth.apple.AppleIdTokenVerifier;
import com.umc.bscene.global.security.oauth.apple.AppleTokenClient;
import com.umc.bscene.global.security.oauth.apple.AppleTokenResponse;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppleMobileOauthFlowTest {

    private static final String APPLE_SUB = "apple-sub";
    private static final String EMAIL = "user@privaterelay.appleid.com";
    private static final String NONCE = "n".repeat(43);
    private static final AppleMobileLoginRequest REQUEST = new AppleMobileLoginRequest(
            "sdk-token", "authorization-code", "c".repeat(43)
    );
    private static final AppleTokenResponse TOKENS = new AppleTokenResponse("exchanged-token", "apple-refresh");

    @Mock
    private AppleChallengeService challenges;
    @Mock
    private AppleIdTokenVerifier verifier;
    @Mock
    private AppleTokenClient tokenClient;
    @Mock
    private ObjectProvider<AppleLoginCompletion> completionProvider;
    @Mock
    private UserRepository users;
    @Mock
    private OauthAccountRepository accounts;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;
    @Mock
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var commonLogin = new OauthService(users, accounts, mock(UserTermsRepository.class),
                mock(TermsRepository.class), mock(PhoneVerificationService.class), redis, jwtUtil);
        // 테스트에서만 B3 완료 지점을 대신하며, 운영에는 저장을 생략하는 구현을 등록하지 않는다.
        when(completionProvider.getIfAvailable()).thenReturn((identity, refreshToken) -> {
            assertThat(refreshToken).isEqualTo(TOKENS.refreshToken());
            return commonLogin.loginWithVerifiedIdentity(identity);
        });
        var service = new AppleMobileOauthService(challenges, verifier, tokenClient, completionProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(new AppleMobileOauthController(challenges, service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void reuseExistingUserWithoutEmailOrNameAndKeepStoredEmail() throws Exception {
        mockIdentity(null);
        var account = mockExistingMember();
        mockTokenIssuance();

        String response = performLogin().andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isNewUser").value(false))
                .andExpect(jsonPath("$.result.token.user.userId").value(42))
                .andExpect(jsonPath("$.result.token.accessToken").value("bscene-access"))
                .andReturn().getResponse().getContentAsString();

        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(response)
                .doesNotContain(REQUEST.identityToken(), REQUEST.authorizationCode(), TOKENS.refreshToken());
        verify(values).set(startsWith("refreshToken:"), eq("42"), any(Duration.class));
        verify(accounts, never()).save(any());
        verifyNoInteractions(users);
    }

    @Test
    void issueOnlyCommonSignupTokenForNewRelayEmailMember() throws Exception {
        mockNewMember(EMAIL);
        when(jwtUtil.createSignupToken("APPLE", APPLE_SUB, null, EMAIL)).thenReturn("signup-token");

        String response = performLogin().andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.signupToken").value("signup-token"))
                .andExpect(jsonPath("$.result.email").value(EMAIL))
                .andReturn().getResponse().getContentAsString();

        assertThat(response)
                .doesNotContain(TOKENS.refreshToken(), REQUEST.identityToken(), REQUEST.authorizationCode());
        verifyNoServiceTokensOrUserCreation();
    }

    @Test
    void rejectNewMemberWithoutSignedEmail() throws Exception {
        mockNewMember(null);
        performLogin().andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OAUTH400_4"));
        verifyNoInteractions(jwtUtil, redis, users);
    }

    @Test
    void rejectInactiveSuspendedAndDeletedMembersBeforeIssuingTokens() throws Exception {
        mockIdentity(null);
        List<UserStatus> statuses = List.of(UserStatus.SUSPENDED, UserStatus.INACTIVE, UserStatus.DELETED);
        List<AuthErrorCode> codes = List.of(
                AuthErrorCode.SUSPENDED_ACCOUNT, AuthErrorCode.INACTIVE_ACCOUNT, AuthErrorCode.DELETED_ACCOUNT
        );
        for (int index = 0; index < statuses.size(); index++) {
            var user = User.builder().id(42L).name("테스트 사용자").status(statuses.get(index)).build();
            var account = OauthAccount.builder().user(user).provider(SocialProvider.APPLE)
                    .providerUid(APPLE_SUB).email(EMAIL).build();
            when(accounts.findByProviderAndProviderUid(SocialProvider.APPLE, APPLE_SUB))
                    .thenReturn(Optional.of(account));
            performLogin().andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(codes.get(index).getCode()));
        }
        verifyNoInteractions(jwtUtil, redis, users);
    }

    private void mockIdentity(String email) {
        var identity = new VerifiedSocialIdentity(SocialProvider.APPLE, APPLE_SUB, email, null);
        when(challenges.getNonce(REQUEST.challengeId())).thenReturn(NONCE);
        when(verifier.verify(REQUEST.identityToken(), NONCE, REQUEST.authorizationCode())).thenReturn(identity);
        when(tokenClient.exchange(REQUEST.authorizationCode())).thenReturn(TOKENS);
        when(verifier.verify(TOKENS.identityToken(), NONCE, REQUEST.authorizationCode())).thenReturn(identity);
    }

    private void mockNewMember(String email) {
        mockIdentity(email);
        when(accounts.findByProviderAndProviderUid(SocialProvider.APPLE, APPLE_SUB)).thenReturn(Optional.empty());
    }

    private OauthAccount mockExistingMember() {
        var account = OauthAccount.builder().user(StreamFixtures.fanUser(42L)).provider(SocialProvider.APPLE)
                .providerUid(APPLE_SUB).email(EMAIL).build();
        when(accounts.findByProviderAndProviderUid(SocialProvider.APPLE, APPLE_SUB)).thenReturn(Optional.of(account));
        return account;
    }

    private void mockTokenIssuance() {
        when(redis.opsForValue()).thenReturn(values);
        when(jwtUtil.createAccessToken(any(AuthMember.class))).thenReturn("bscene-access");
        when(jwtUtil.createRefreshToken(any(AuthMember.class))).thenReturn("bscene-refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(1209600000L);
    }

    private ResultActions performLogin() throws Exception {
        String body = """
                {"identityToken":"%s","authorizationCode":"%s","challengeId":"%s"}
                """.formatted(REQUEST.identityToken(), REQUEST.authorizationCode(), REQUEST.challengeId());
        return mockMvc.perform(post("/auth/oauth/mobile/apple").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void verifyNoServiceTokensOrUserCreation() {
        verify(jwtUtil, never()).createAccessToken(any());
        verify(jwtUtil, never()).createRefreshToken(any());
        verify(accounts, never()).save(any());
        verifyNoInteractions(users, redis);
    }
}
