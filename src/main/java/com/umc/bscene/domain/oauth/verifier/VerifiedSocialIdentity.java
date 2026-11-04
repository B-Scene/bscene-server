package com.umc.bscene.domain.oauth.verifier;

import com.umc.bscene.domain.oauth.enums.SocialProvider;

/**
 * 제공자 토큰을 서버가 직접 검증해 얻은 소셜 신원.
 * 카카오·구글·Apple 검증기가 공통으로 만들어 OauthService.loginWithVerifiedIdentity 에 넘긴다.
 * 반드시 서버 검증 코드만 생성한다. FE 요청 본문의 providerUid/email 을 받아 만들지 않는다.
 *
 * @param provider    검증한 제공자
 * @param providerUid 제공자가 보장하는 사용자 고유 식별자 (카카오 id / 구글 sub / Apple sub)
 * @param email       제공자 응답의 이메일. 기존 회원은 providerUid 로 식별하므로 null 허용.
 *                    신규 가입 분기에서만 필수이며, 누락·공백이면 OAUTH400_4 로 거절된다
 * @param name        제공자 응답의 표시 이름. 없으면 null (가입 시 이름은 별도 입력)
 */
public record VerifiedSocialIdentity(
        SocialProvider provider,
        String providerUid,
        String email,
        String name
) {
}
