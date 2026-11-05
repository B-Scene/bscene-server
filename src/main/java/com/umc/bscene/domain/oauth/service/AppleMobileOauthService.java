package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.request.AppleMobileLoginRequest;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.security.oauth.apple.AppleIdTokenVerifier;
import com.umc.bscene.global.security.oauth.apple.AppleTokenClient;
import com.umc.bscene.global.security.oauth.apple.AppleTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppleMobileOauthService {

    private final AppleChallengeService challenges;
    private final AppleIdTokenVerifier verifier;
    private final AppleTokenClient tokenClient;
    private final ObjectProvider<AppleLoginCompletion> completionProvider;

    public OauthLoginResponse login(AppleMobileLoginRequest request) {
        AppleLoginCompletion completion = requireCompletion();
        completion.validateConfiguration();
        String nonce = challenges.getNonce(request.challengeId());
        VerifiedSocialIdentity identity = verifier.verify(request.identityToken(), nonce, request.authorizationCode());
        LocalDateTime loginDeadline = completion.begin(identity);
        challenges.consume(request.challengeId(), nonce);
        AppleTokenResponse tokens = tokenClient.exchange(request.authorizationCode());
        return finishExchange(completion, identity, tokens, nonce, request.authorizationCode(), loginDeadline);
    }

    private AppleLoginCompletion requireCompletion() {
        AppleLoginCompletion completion = completionProvider.getIfAvailable();
        if (completion == null) {
            throw new IllegalStateException("Apple 자격정보 보관·가입 연결 구현이 필요합니다.");
        }
        return completion;
    }

    private OauthLoginResponse finishExchange(
            AppleLoginCompletion completion, VerifiedSocialIdentity identity, AppleTokenResponse tokens,
            String nonce, String authorizationCode, LocalDateTime loginDeadline
    ) {
        String attemptId = completion.stage(tokens, loginDeadline);
        try {
            completion.identify(attemptId, verifier.verifyForCleanup(tokens.identityToken()));
            VerifiedSocialIdentity exchanged = verifier.verify(tokens.identityToken(), nonce, authorizationCode);
            if (!identity.providerUid().equals(exchanged.providerUid())) {
                throw new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
            }
            return completion.complete(attemptId, identity);
        } catch (RuntimeException exception) {
            completion.reject(attemptId);
            throw exception;
        }
    }
}
