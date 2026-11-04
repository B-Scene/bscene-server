package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;

/**
 * B3 구현은 Apple 자격정보를 안전하게 보관한 뒤 공통 로그인을 호출한다.
 * 신규 가입은 signupToken과 보관 자료를 연결하며, 저장·연결 실패 시 성공을 반환하지 않는다.
 * 구현체가 없으면 Apple 로그인은 코드 소비·비씬 토큰 발급 전에 차단된다.
 */
public interface AppleLoginCompletion {

    OauthLoginResponse complete(VerifiedSocialIdentity identity, String refreshToken);
}
