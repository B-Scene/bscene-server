package com.umc.bscene.global.security.service;

import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.dto.response.GoogleResponse;
import com.umc.bscene.global.security.dto.response.KakaoResponse;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.OAuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

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
            throw new BaseException(GeneralErrorCode.NOT_SUPPORT_SOCIAL_PROVIDER);
        }

        // provider별로 응답 구조가 달라 각 case 안에서 파싱
        String providerUid;
        OAuthResponse dto;
        switch (provider) {
            case KAKAO -> {
                providerUid = String.valueOf((Long) oAuthMember.getAttribute("id"));
                Map<String, Object> account = oAuthMember.getAttribute("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) account.get("profile");
                String email = account.get("email").toString();
                String name = profile.get("nickname").toString();
                dto = new KakaoResponse(providerUid, email, name);
            }
            case GOOGLE -> {
                // 구글은 평평한 구조 (OpenID Connect) — providerUid는 "sub"
                providerUid = oAuthMember.getAttribute("sub").toString();
                String email = oAuthMember.getAttribute("email").toString();
                String name = oAuthMember.getAttribute("name").toString();
                dto = new GoogleResponse(providerUid, email, name);
            }
            default -> throw new BaseException(GeneralErrorCode.NOT_SUPPORT_SOCIAL_PROVIDER);
        }

        // DB 저장: 있다면 그 데이터 가져오고 없으면 새로 저장
        User user = userRepository.findByProviderAndProviderUid(provider, providerUid)
                .orElseGet(() -> {
                    User newUser = UserConverter.toUser(dto);
                    userRepository.save(newUser);
                    return newUser;
                });
        return new OAuthMember(user, oAuthMember.getAttributes());
    }
}