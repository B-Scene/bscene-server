package com.umc.bscene.global.security.service;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.global.security.dto.response.GoogleResponse;
import com.umc.bscene.global.security.dto.response.KakaoResponse;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.OAuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuthService extends DefaultOAuth2UserService {

    private final OauthAccountRepository oauthAccountRepository;

    @Override
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {
        // (필수) 인증 서버의 일회성 토큰을 이용해 정보 조회 & 유저 객체 생성
        OAuth2User oAuthMember = super.loadUser(userRequest);

        // provider 판별 (provider 공통)
        SocialProvider provider;
        try {
            provider = SocialProvider.valueOf(
                    userRequest.getClientRegistration().getRegistrationId().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw oauthAuthException(OauthErrorCode.NOT_SUPPORT_PROVIDER);
        }

        // provider별로 응답 구조가 달라 각 case 안에서 파싱
        OAuthResponse dto = switch (provider) {
            case KAKAO -> parseKakao(oAuthMember);
            case GOOGLE -> parseGoogle(oAuthMember);
            case APPLE -> throw oauthAuthException(OauthErrorCode.NOT_SUPPORT_PROVIDER);
        };

        // OauthAccount 조회: 있으면 기존 유저, 없으면 신규 유저(온보딩 필요)
        return oauthAccountRepository.findByProviderAndProviderUid(provider, dto.getProviderUid())
                .map(account -> OAuthMember.ofExisting(account.getUser(), dto, oAuthMember.getAttributes()))
                .orElseGet(() -> OAuthMember.ofNew(dto, oAuthMember.getAttributes()));
    }

    private OAuthResponse parseKakao(OAuth2User oAuthMember) {
        Object id = oAuthMember.getAttribute("id");
        if (id == null) {
            throw oauthAuthException(OauthErrorCode.NOT_SUPPORT_PROVIDER);
        }
        String providerUid = String.valueOf(id);

        // 이메일은 아이디로 쓰이므로 필수. 미동의 시 kakao_account.email 자체가 없음
        Map<String, Object> account = oAuthMember.getAttribute("kakao_account");
        Object email = (account == null) ? null : account.get("email");
        if (email == null) {
            throw oauthAuthException(OauthErrorCode.EMAIL_NOT_PROVIDED);
        }

        // 닉네임은 아이디가 아니라 필수 아님(회원가입 시 이름을 직접 입력받음). 없으면 null 허용
        String name = extractKakaoNickname(account);

        return new KakaoResponse(providerUid, email.toString(), name);
    }

    private OAuthResponse parseGoogle(OAuth2User oAuthMember) {
        Object sub = oAuthMember.getAttribute("sub");
        if (sub == null) {
            throw oauthAuthException(OauthErrorCode.NOT_SUPPORT_PROVIDER);
        }

        Object email = oAuthMember.getAttribute("email");
        if (email == null) {
            throw oauthAuthException(OauthErrorCode.EMAIL_NOT_PROVIDED);
        }

        Object name = oAuthMember.getAttribute("name");
        return new GoogleResponse(sub.toString(), email.toString(), name == null ? null : name.toString());
    }

    private String extractKakaoNickname(Map<String, Object> account) {
        Object profile = account.get("profile");
        if (profile instanceof Map<?, ?> profileMap) {
            Object nickname = profileMap.get("nickname");
            return nickname == null ? null : nickname.toString();
        }
        return null;
    }

    // 필터 계층 예외 → OAuth2AuthenticationException에 에러 코드를 담아 실패 핸들러(OAuthFailureHandler)가 읽게 함
    private OAuth2AuthenticationException oauthAuthException(OauthErrorCode errorCode) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(errorCode.getCode(), errorCode.getMessage(), null),
                errorCode.getMessage()
        );
    }
}
