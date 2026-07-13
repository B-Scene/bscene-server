package com.umc.bscene.domain.auth.config;

import com.umc.bscene.domain.auth.adapter.StreamAdapter;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
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
}
