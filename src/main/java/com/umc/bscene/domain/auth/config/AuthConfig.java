package com.umc.bscene.domain.auth.config;

import com.umc.bscene.domain.auth.adapter.StreamAdapter;
import com.umc.bscene.domain.auth.adapter.UserAdapter;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.user.port.AuthPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {
    @Bean
    public StreamAdapter streamUserTermsAdapter(
            UserTermsRepository userTermsRepository,
            @Value("${notification.terms.live-id}") Long liveNotificationTermsId
    ) {
        return new StreamAdapter(
                userTermsRepository,
                liveNotificationTermsId
        );
    }

    @Bean
    public AuthPort authPort(
            LocalCredentialRepository localCredentialRepository,
            OauthAccountRepository oauthAccountRepository
    ) {
        return new UserAdapter(
                localCredentialRepository,
                oauthAccountRepository
        );
    }
}
