package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.auth.dto.auth.response.LoginUserResponse;
import com.umc.bscene.domain.auth.dto.auth.response.TokenResponse;
import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
import com.umc.bscene.domain.oauth.dto.request.GoogleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.security.oauth.google.GoogleIdTokenVerifier;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class GoogleMobileOauthControllerTest {

    private static final String PATH = "/auth/oauth/mobile/google";
    private static final String ID_TOKEN = "test-google-id-token";
    private static final String REQUEST = "{\"idToken\":\"" + ID_TOKEN + "\"}";
    private static final VerifiedSocialIdentity IDENTITY = new VerifiedSocialIdentity(
            SocialProvider.GOOGLE, "google-sub", "member@example.com", "테스트 사용자"
    );

    @Mock
    private GoogleIdTokenVerifier verifier;
    @Mock
    private OauthService oauthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GoogleMobileOauthController(verifier, oauthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnCommonLoginResponseForExistingMember() throws Exception {
        TokenResponse token = createExistingMemberToken();
        when(verifier.verify(ID_TOKEN)).thenReturn(IDENTITY);
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
    void returnSignupTokenAndEmailForNewMember() throws Exception {
        when(verifier.verify(ID_TOKEN)).thenReturn(IDENTITY);
        when(oauthService.loginWithVerifiedIdentity(IDENTITY))
                .thenReturn(OauthLoginResponse.ofNew("signup-token", "member@example.com"));

        performLogin(REQUEST)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OAUTH200_1"))
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.signupToken").value("signup-token"))
                .andExpect(jsonPath("$.result.email").value("member@example.com"))
                .andExpect(jsonPath("$.result.token").value(nullValue()));
    }

    @Test
    void rejectInvalidBodiesBeforeAuthentication() throws Exception {
        List<String> bodies = List.of(
                "", "{}", "{\"idToken\":null}", "{\"idToken\":\"   \"}",
                "{\"idToken\":{}}", "{\"idToken\":\"" + "a".repeat(16385) + "\"}",
                "{\"idToken\":\"" + ID_TOKEN + "\","
        );

        for (String body : bodies) {
            String response = performLogin(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400_2"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain(ID_TOKEN);
        }
        verifyNoInteractions(verifier, oauthService);
    }

    @Test
    void rejectInvalidProviderTokenBeforeMemberLookup() throws Exception {
        assertProviderFailure(OauthErrorCode.INVALID_PROVIDER_TOKEN, 401, "OAUTH401_1");
    }

    @Test
    void returnAppMismatchForAnotherClientToken() throws Exception {
        assertProviderFailure(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, 401, "OAUTH401_2");
    }

    @Test
    void returnServiceUnavailableForProviderFailure() throws Exception {
        assertProviderFailure(OauthErrorCode.PROVIDER_UNAVAILABLE, 503, "OAUTH503_1");
    }

    @Test
    void returnCommonAccountStatusFailure() throws Exception {
        when(verifier.verify(ID_TOKEN)).thenReturn(IDENTITY);
        when(oauthService.loginWithVerifiedIdentity(IDENTITY))
                .thenThrow(new AuthException(AuthErrorCode.SUSPENDED_ACCOUNT));

        performLogin(REQUEST)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH403_1"));
    }

    @Test
    void redactTokenInRequestString() {
        assertThat(new GoogleMobileLoginRequest(ID_TOKEN).toString()).doesNotContain(ID_TOKEN);
    }

    private TokenResponse createExistingMemberToken() {
        return new TokenResponse(
                "Bearer", "bscene-access", "bscene-refresh", 3600000L,
                new LoginUserResponse(42L, "테스트 사용자", UserMode.FAN, true)
        );
    }

    private ResultActions performLogin(String body) throws Exception {
        return mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertProviderFailure(OauthErrorCode error, int httpStatus, String code) throws Exception {
        when(verifier.verify(ID_TOKEN)).thenThrow(new OauthException(error));

        String response = performLogin(REQUEST)
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(ID_TOKEN);
        verifyNoInteractions(oauthService);
    }
}
