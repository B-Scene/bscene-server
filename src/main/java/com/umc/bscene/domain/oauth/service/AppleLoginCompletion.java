package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import com.umc.bscene.global.security.oauth.apple.AppleTokenResponse;
import java.time.LocalDateTime;

/**
 * 외부 코드 소비 후 받은 자료를 먼저 보관하고, 검증·가입·실패 정리를 연결한다.
 */
public interface AppleLoginCompletion {

    void validateConfiguration();

    LocalDateTime begin(VerifiedSocialIdentity identity);

    String stage(AppleTokenResponse tokens, LocalDateTime loginDeadline);

    void identify(String attemptId, VerifiedSocialIdentity signedIdentity);

    OauthLoginResponse complete(String attemptId, VerifiedSocialIdentity identity);

    void reject(String attemptId);
}
