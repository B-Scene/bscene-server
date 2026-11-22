package com.umc.bscene.domain.oauth.dto.response;

public record AppleChallengeResponse(String challengeId, String nonce, long expiresInSeconds) {

    @Override
    public String toString() {
        return "AppleChallengeResponse[REDACTED, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
