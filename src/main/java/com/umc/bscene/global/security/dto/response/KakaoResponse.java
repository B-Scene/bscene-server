package com.umc.bscene.global.security.dto.response;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoResponse implements OAuthResponse {

    private final String id;
    private final String email;
    private final String name;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String getProviderUid() {
        return id;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }
}