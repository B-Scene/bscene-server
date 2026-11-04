package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.request.AppleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.security.oauth.apple.AppleIdTokenVerifier;
import com.umc.bscene.global.security.oauth.apple.AppleTokenClient;
import com.umc.bscene.global.security.oauth.apple.AppleTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleMobileOauthServiceTest {

    private static final AppleMobileLoginRequest REQUEST = new AppleMobileLoginRequest(
            "sdk-identity-token", "authorization-code", "c".repeat(43)
    );
    private static final String NONCE = "n".repeat(43);
    private static final VerifiedSocialIdentity IDENTITY = new VerifiedSocialIdentity(
            SocialProvider.APPLE, "apple-sub", "user@privaterelay.appleid.com", null
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
    private AppleLoginCompletion completion;

    private AppleMobileOauthService service;

    @BeforeEach
    void setUp() {
        service = new AppleMobileOauthService(challenges, verifier, tokenClient, completionProvider);
    }

    @Test
    void blockLoginBeforeConsumptionWhenCredentialCompletionIsMissing() {
        assertThrows(IllegalStateException.class, () -> service.login(REQUEST));
        verifyNoInteractions(challenges, verifier, tokenClient, completion);
    }

    @Test
    void verifyConsumeExchangeAndCompleteInOrder() {
        mockInitialVerification();
        mockExchangeVerification();
        OauthLoginResponse expected = OauthLoginResponse.ofNew("signup-token", IDENTITY.email());
        when(completion.complete(IDENTITY, TOKENS.refreshToken())).thenReturn(expected);

        assertThat(service.login(REQUEST)).isSameAs(expected);

        var order = inOrder(challenges, verifier, tokenClient, completion);
        order.verify(challenges).getNonce(REQUEST.challengeId());
        order.verify(verifier).verify(REQUEST.identityToken(), NONCE, REQUEST.authorizationCode());
        order.verify(challenges).consume(REQUEST.challengeId(), NONCE);
        order.verify(tokenClient).exchange(REQUEST.authorizationCode());
        order.verify(verifier).verify(TOKENS.identityToken(), NONCE, REQUEST.authorizationCode());
        order.verify(completion).complete(IDENTITY, TOKENS.refreshToken());
    }

    @Test
    void rejectInvalidInitialTokenWithoutConsumingChallengeOrCode() {
        when(completionProvider.getIfAvailable()).thenReturn(completion);
        when(challenges.getNonce(REQUEST.challengeId())).thenReturn(NONCE);
        when(verifier.verify(REQUEST.identityToken(), NONCE, REQUEST.authorizationCode()))
                .thenThrow(new OauthException(OauthErrorCode.APPLE_NONCE_MISMATCH));

        assertThrows(OauthException.class, () -> service.login(REQUEST));
        verify(challenges, times(0)).consume(REQUEST.challengeId(), NONCE);
        verifyNoInteractions(tokenClient, completion);
    }

    @Test
    void rejectConsumedChallengeBeforeExternalExchange() {
        mockInitialVerification();
        doThrow(new OauthException(OauthErrorCode.INVALID_APPLE_CHALLENGE))
                .when(challenges).consume(REQUEST.challengeId(), NONCE);

        assertThrows(OauthException.class, () -> service.login(REQUEST));
        verifyNoInteractions(tokenClient, completion);
    }

    @Test
    void rejectCodeExchangeFailureWithoutLoginOrAutomaticRetry() {
        mockInitialVerification();
        when(tokenClient.exchange(REQUEST.authorizationCode()))
                .thenThrow(new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE));

        assertThrows(OauthException.class, () -> service.login(REQUEST));
        verify(tokenClient).exchange(REQUEST.authorizationCode());
        verifyNoInteractions(completion);
    }

    @Test
    void rejectExchangedTokenFromAnotherUser() {
        mockInitialVerification();
        when(tokenClient.exchange(REQUEST.authorizationCode())).thenReturn(TOKENS);
        when(verifier.verify(TOKENS.identityToken(), NONCE, REQUEST.authorizationCode()))
                .thenReturn(new VerifiedSocialIdentity(SocialProvider.APPLE, "other-user", IDENTITY.email(), null));

        OauthException error = assertThrows(OauthException.class, () -> service.login(REQUEST));
        assertThat(error.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_PROVIDER_TOKEN);
        verifyNoInteractions(completion);
    }

    @Test
    void rejectExchangedTokenFromAnotherAttempt() {
        mockInitialVerification();
        when(tokenClient.exchange(REQUEST.authorizationCode())).thenReturn(TOKENS);
        when(verifier.verify(TOKENS.identityToken(), NONCE, REQUEST.authorizationCode()))
                .thenThrow(new OauthException(OauthErrorCode.APPLE_NONCE_MISMATCH));

        assertThrows(OauthException.class, () -> service.login(REQUEST));
        verifyNoInteractions(completion);
    }

    @Test
    void propagateCredentialPersistenceFailureWithoutSuccess() {
        mockInitialVerification();
        mockExchangeVerification();
        when(completion.complete(IDENTITY, TOKENS.refreshToken())).thenThrow(new IllegalStateException("저장 실패"));

        assertThrows(IllegalStateException.class, () -> service.login(REQUEST));
    }

    @Test
    void allowOnlyOneConcurrentRequestPastAtomicConsumption() throws Exception {
        mockInitialVerification();
        mockExchangeVerification();
        mockAtomicConsumption();
        when(completion.complete(IDENTITY, TOKENS.refreshToken()))
                .thenReturn(OauthLoginResponse.ofNew("signup-token", IDENTITY.email()));

        assertThat(executeConcurrentLogins()).isEqualTo(1);
        verify(tokenClient).exchange(REQUEST.authorizationCode());
        verify(completion).complete(IDENTITY, TOKENS.refreshToken());
    }

    private long executeConcurrentLogins() throws Exception {
        var requests = new ArrayList<Callable<Boolean>>();
        for (int index = 0; index < 12; index++) {
            requests.add(() -> attemptLogin());
        }
        try (var executor = Executors.newFixedThreadPool(12)) {
            long successes = 0;
            for (var result : executor.invokeAll(requests)) {
                successes += result.get() ? 1 : 0;
            }
            return successes;
        }
    }

    private void mockAtomicConsumption() {
        AtomicBoolean consumed = new AtomicBoolean();
        doAnswer(invocation -> {
            if (!consumed.compareAndSet(false, true)) {
                throw new OauthException(OauthErrorCode.INVALID_APPLE_CHALLENGE);
            }
            return null;
        }).when(challenges).consume(REQUEST.challengeId(), NONCE);
    }

    private boolean attemptLogin() {
        try {
            service.login(REQUEST);
            return true;
        } catch (OauthException exception) {
            assertThat(exception.getBaseResponseCode()).isEqualTo(OauthErrorCode.INVALID_APPLE_CHALLENGE);
            return false;
        }
    }

    private void mockInitialVerification() {
        when(completionProvider.getIfAvailable()).thenReturn(completion);
        when(challenges.getNonce(REQUEST.challengeId())).thenReturn(NONCE);
        when(verifier.verify(REQUEST.identityToken(), NONCE, REQUEST.authorizationCode())).thenReturn(IDENTITY);
    }

    private void mockExchangeVerification() {
        when(tokenClient.exchange(REQUEST.authorizationCode())).thenReturn(TOKENS);
        when(verifier.verify(TOKENS.identityToken(), NONCE, REQUEST.authorizationCode())).thenReturn(IDENTITY);
    }
}
