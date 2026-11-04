package com.umc.bscene.domain.oauth.controller;

import com.umc.bscene.domain.oauth.dto.request.AppleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.AppleChallengeResponse;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.AppleOauthErrorCode;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.service.AppleChallengeService;
import com.umc.bscene.domain.oauth.service.AppleMobileOauthService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppleMobileOauthControllerTest {

    private static final String PATH = "/auth/oauth/mobile/apple";
    private static final String CHALLENGE_ID = "c".repeat(43);
    private static final String ID_TOKEN = "sensitive-sdk-token";
    private static final String CODE = "sensitive-authorization-code";
    private static final String REQUEST = """
            {"identityToken":"%s","authorizationCode":"%s","challengeId":"%s"}
            """.formatted(ID_TOKEN, CODE, CHALLENGE_ID);

    @Mock
    private AppleChallengeService challenges;
    @Mock
    private AppleMobileOauthService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AppleMobileOauthController(challenges, service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void issueChallengeWithCommonEnvelopeAndSeconds() throws Exception {
        when(challenges.issue()).thenReturn(new AppleChallengeResponse(CHALLENGE_ID, "n".repeat(43), 300));
        mockMvc.perform(post(PATH + "/challenge").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OAUTH200_2"))
                .andExpect(jsonPath("$.result.challengeId").value(CHALLENGE_ID))
                .andExpect(jsonPath("$.result.nonce").value("n".repeat(43)))
                .andExpect(jsonPath("$.result.expiresInSeconds").value(300));
        verifyNoInteractions(service);
    }

    @Test
    void requireObjectBodyForChallenge() throws Exception {
        for (String body : List.of("", "null", "[]", "{", "\"string\"")) {
            mockMvc.perform(post(PATH + "/challenge").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400_2"));
        }
        verifyNoInteractions(challenges, service);
    }

    @Test
    void returnCommonSignupResponseWithoutProviderCredentials() throws Exception {
        when(service.login(any()))
                .thenReturn(OauthLoginResponse.ofNew("signup-token", "user@privaterelay.appleid.com"));

        String response = performLogin(REQUEST)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OAUTH200_1"))
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.signupToken").value("signup-token"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(ID_TOKEN, CODE, "identityToken", "authorizationCode");
    }

    @Test
    void rejectMissingMalformedAndOversizedLoginBodies() throws Exception {
        List<String> bodies = List.of(
                "", "{}", "null", REQUEST.replace(ID_TOKEN, " "), REQUEST.replace(CODE, ""),
                REQUEST.replace(CHALLENGE_ID, "invalid-id"), REQUEST.replace(ID_TOKEN, "a".repeat(16385)),
                REQUEST.replace(CODE, "a".repeat(4097)), REQUEST.replace(CODE, "non ascii 한글"),
                REQUEST.replace("\"" + ID_TOKEN + "\"", "{}"), REQUEST.substring(0, REQUEST.length() - 3)
        );
        for (String body : bodies) {
            String response = performLogin(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400_2"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain(ID_TOKEN, CODE);
        }
        verifyNoInteractions(service, challenges);
    }

    @Test
    void mapAuthenticationAndStorageFailuresToDocumentedErrors() throws Exception {
        var errors = List.of(
                OauthErrorCode.INVALID_APPLE_CHALLENGE, OauthErrorCode.INVALID_PROVIDER_TOKEN,
                OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH, OauthErrorCode.APPLE_NONCE_MISMATCH,
                OauthErrorCode.PROVIDER_UNAVAILABLE, AppleOauthErrorCode.AUTH_STORE_UNAVAILABLE
        );
        for (var error : errors) {
            doThrow(new OauthException(error)).when(service).login(any());
            String response = performLogin(REQUEST)
                    .andExpect(status().is(error.getStatus()))
                    .andExpect(jsonPath("$.code").value(error.getCode()))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain(ID_TOKEN, CODE);
        }
    }

    @Test
    void failClosedWhenCredentialCompletionIsNotConfigured() throws Exception {
        when(service.login(any())).thenThrow(new IllegalStateException("Apple 자격정보 보관·가입 연결 구현이 필요합니다."));
        performLogin(REQUEST)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_500_1"));
    }

    @Test
    void mapChallengeStorageFailureWithoutSuccess() throws Exception {
        when(challenges.issue()).thenThrow(new OauthException(AppleOauthErrorCode.AUTH_STORE_UNAVAILABLE));
        mockMvc.perform(post(PATH + "/challenge").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("COMMON_503_1"));
    }

    @Test
    void redactRequestCredentialsInStringRepresentation() {
        assertThat(new AppleMobileLoginRequest(ID_TOKEN, CODE, CHALLENGE_ID).toString())
                .doesNotContain(ID_TOKEN, CODE, CHALLENGE_ID);
    }

    private ResultActions performLogin(String body) throws Exception {
        return mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
