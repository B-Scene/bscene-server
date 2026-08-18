package com.umc.bscene.global.security.dto.response;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AppleResponse implements OAuthResponse{

    private final String sub;
    private final String email;
    private final String name;


    @Override
    public SocialProvider getProvider() {
        return SocialProvider.APPLE;
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
