package com.umc.bscene.global.security.dto.response;

import com.umc.bscene.domain.oauth.enums.SocialProvider;

public interface OAuthResponse {
    SocialProvider getProvider();
    String getProviderUid();
    String getEmail();
    String getName();
}