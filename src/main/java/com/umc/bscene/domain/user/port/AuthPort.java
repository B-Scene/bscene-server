package com.umc.bscene.domain.user.port;

public interface AuthPort {

    String getEmailToLocalCredential();
    String getEmailToOauthAccount();
}
