package com.umc.bscene.global.security.oauth.apple;

public record AppleTokenResponse(String identityToken, String refreshToken) {

    @Override
    public String toString() {
        return "AppleTokenResponse[identityToken=REDACTED, refreshToken=REDACTED]";
    }
}
