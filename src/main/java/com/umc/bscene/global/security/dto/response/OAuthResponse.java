package com.umc.bscene.global.security.dto.response;

public interface OAuthResponse {
    SocialProvider getProvider();
    String getProviderUid();
    String getEmail();
    String getName();
}