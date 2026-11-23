package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.auth.dto.auth.response.LoginUserResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.oauth.dto.request.KakaoMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.exception.GlobalExceptionHandler;
import com.umc.bscene.global.security.oauth.kakao.KakaoAccessTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 구글 컨트롤러 테스트와 같은 구조. 검증기·서비스는 목이고 컨트롤러 + GlobalExceptionHandler 의 응답 변환만 본다.
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoMobileOauthController")
class KakaoMobileOauthControllerTest {

    private static final String PATH = "/auth/oauth/mobile/kakao";
    private static final String ACCESS_TOKEN = "test-kakao-access-token";
    private static final String REQUEST = "{\"accessToken\":\"" + ACCESS_TOKEN + "\"}";
    private static final VerifiedSocialIdentity IDENTITY = new VerifiedSocialIdentity(
            SocialProvider.KAKAO, "123456", "sejin@kakao.com", "세진"
    );

    @Mock
    private KakaoAccessTokenVerifier verifier;
    @Mock
    private OauthService oauthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new KakaoMobileOauthController(verifier, oauthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("기존 회원이면 비씬 토큰을 담은 공통 로그인 응답을 돌려준다")
    void returnCommonLoginResponseForExistingMember() throws Exception {
        TokenResponse token = createExistingMemberToken();
        when(verifier.verify(ACCESS_TOKEN)).thenReturn(IDENTITY);
        when(oauthService.loginWithVerifiedIdentity(IDENTITY)).thenReturn(OauthLoginResponse.ofExisting(token));

        performLogin(REQUEST)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OAUTH200_1"))
                .andExpect(jsonPath("$.result.isNewUser").value(false))
                .andExpect(jsonPath("$.result.email").value(nullValue()))
                .andExpect(jsonPath("$.result.signupToken").value(nullValue()))
                .andExpect(jsonPath("$.result.token.accessToken").value("bscene-access"))
                .andExpect(jsonPath("$.result.token.refreshToken").value("bscene-refresh"))
                .andExpect(jsonPath("$.result.token.user.userId").value(42));

        verify(oauthService).loginWithVerifiedIdentity(IDENTITY);
    }

    @Test
    @DisplayName("신규 회원이면 signupToken 과 이메일을 돌려준다")
    void returnSignupTokenAndEmailForNewMember() throws Exception {
        when(verifier.verify(ACCESS_TOKEN)).thenReturn(IDENTITY);
        when(oauthService.loginWithVerifiedIdentity(IDENTITY))
                .thenReturn(OauthLoginResponse.ofNew("signup-token", "sejin@kakao.com"));

        performLogin(REQUEST)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OAUTH200_1"))
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.signupToken").value("signup-token"))
                .andExpect(jsonPath("$.result.email").value("sejin@kakao.com"))
                .andExpect(jsonPath("$.result.token").value(nullValue()));
    }

    @Test
    @DisplayName("본문이 잘못되면 검증기를 부르기 전에 400 COMMON_400_2 로 거절하고 토큰을 응답에 남기지 않는다")
    void rejectInvalidBodiesBeforeAuthentication() throws Exception {
        List<String> bodies = List.of(
                "", "{}", "{\"accessToken\":null}", "{\"accessToken\":\"   \"}",
                "{\"accessToken\":{}}", "{\"accessToken\":\"" + "a".repeat(4097) + "\"}",
                "{\"accessToken\":\"" + ACCESS_TOKEN + "\","
        );

        for (String body : bodies) {
            String response = performLogin(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400_2"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain(ACCESS_TOKEN);
        }
        verifyNoInteractions(verifier, oauthService);
    }

    @Test
    @DisplayName("토큰이 무효·만료면 회원 조회 없이 401 OAUTH401_1")
    void rejectInvalidProviderTokenBeforeMemberLookup() throws Exception {
        assertProviderFailure(OauthErrorCode.INVALID_PROVIDER_TOKEN, 401, "OAUTH401_1");
    }

    @Test
    @DisplayName("다른 카카오 앱 토큰이면 401 OAUTH401_2")
    void returnAppMismatchForAnotherAppToken() throws Exception {
        assertProviderFailure(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, 401, "OAUTH401_2");
    }

    @Test
    @DisplayName("카카오 장애면 503 OAUTH503_1")
    void returnServiceUnavailableForProviderFailure() throws Exception {
        assertProviderFailure(OauthErrorCode.PROVIDER_UNAVAILABLE, 503, "OAUTH503_1");
    }

    @Test
    @DisplayName("신규 회원인데 이메일이 없으면 공통 처리의 400 OAUTH400_4 가 그대로 전달된다")
    void returnEmailNotProvidedForNewMemberWithoutEmail() throws Exception {
        VerifiedSocialIdentity withoutEmail = new VerifiedSocialIdentity(SocialProvider.KAKAO, "123456", null, "세진");
        when(verifier.verify(ACCESS_TOKEN)).thenReturn(withoutEmail);
        when(oauthService.loginWithVerifiedIdentity(withoutEmail))
                .thenThrow(new OauthException(OauthErrorCode.EMAIL_NOT_PROVIDED));

        performLogin(REQUEST)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OAUTH400_4"));
    }

    @Test
    @DisplayName("정지 계정이면 403 AUTH403_1")
    void returnCommonAccountStatusFailure() throws Exception {
        when(verifier.verify(ACCESS_TOKEN)).thenReturn(IDENTITY);
        when(oauthService.loginWithVerifiedIdentity(IDENTITY))
                .thenThrow(new AuthException(AuthErrorCode.SUSPENDED_ACCOUNT));

        performLogin(REQUEST)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH403_1"));
    }

    @Test
    @DisplayName("앱 ID 설정이 없으면 500 COMMON_500_1 이고 토큰은 응답에 없다")
    void returnServerErrorWhenMisconfigured() throws Exception {
        when(verifier.verify(ACCESS_TOKEN))
                .thenThrow(new IllegalStateException("카카오 모바일 OAuth 앱 ID(KAKAO_APP_ID) 설정이 필요합니다."));

        String response = performLogin(REQUEST)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_500_1"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(ACCESS_TOKEN);
        verifyNoInteractions(oauthService);
    }

    @Test
    @DisplayName("요청 DTO 의 toString 에 토큰이 노출되지 않는다")
    void redactTokenInRequestString() {
        assertThat(new KakaoMobileLoginRequest(ACCESS_TOKEN).toString()).doesNotContain(ACCESS_TOKEN);
    }

    private TokenResponse createExistingMemberToken() {
        return new TokenResponse(
                "Bearer", "bscene-access", "bscene-refresh", 3600000L,
                new LoginUserResponse(42L, "세진", UserMode.FAN, true)
        );
    }

    private ResultActions performLogin(String body) throws Exception {
        return mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertProviderFailure(OauthErrorCode error, int httpStatus, String code) throws Exception {
        when(verifier.verify(ACCESS_TOKEN)).thenThrow(new OauthException(error));

        String response = performLogin(REQUEST)
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(ACCESS_TOKEN);
        verifyNoInteractions(oauthService);
    }
}
