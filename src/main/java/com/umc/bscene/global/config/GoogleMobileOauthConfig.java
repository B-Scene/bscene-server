package com.umc.bscene.global.config;

import com.umc.bscene.global.security.oauth.google.GoogleIdTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
public class GoogleMobileOauthConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${oauth.mobile.google.allowed-audiences:}") String allowedAudiences
    ) {
        var audiences = Arrays.stream(allowedAudiences.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new GoogleIdTokenVerifier(audiences);
    }
}
