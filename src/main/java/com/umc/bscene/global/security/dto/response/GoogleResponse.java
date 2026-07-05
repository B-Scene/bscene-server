package com.umc.bscene.global.security.dto.response;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleResponse implements OAuthResponse {

    private final String sub;   // 구글 고유 식별자 (providerUid)
    private final String email;
    private final String name;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public String getProviderUid() {
        return sub;
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