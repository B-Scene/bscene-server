package com.umc.bscene.domain.user.port;

public interface AuthPort {

    boolean hasLocalCredential(Long userId);
    boolean hasOauthAccount(Long userId);

    String getEmailToLocalCredential(Long userId);
    String getEmailToOauthAccount(Long userId);
}
