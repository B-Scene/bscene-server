package com.umc.bscene.domain.user.port;

public interface AuthPort {

    String getEmailToLocalCredential(Long userId);
    String getEmailToOauthAccount(Long userId);
}
