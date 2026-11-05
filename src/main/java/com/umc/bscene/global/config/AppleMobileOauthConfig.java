package com.umc.bscene.global.config;

import com.umc.bscene.global.security.oauth.apple.AppleClientSecretGenerator;
import com.umc.bscene.global.security.oauth.apple.AppleCredentialCipher;
import com.umc.bscene.global.security.oauth.apple.AppleIdTokenVerifier;
import com.umc.bscene.global.security.oauth.apple.AppleTokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppleMobileOauthConfig {

    @Bean
    public AppleCredentialCipher appleCredentialCipher(
            @Value("${oauth.mobile.apple.credential-encryption-key:${APPLE_CREDENTIAL_ENCRYPTION_KEY:}}") String key
    ) {
        return new AppleCredentialCipher(key.trim());
    }

    @Bean
    public AppleIdTokenVerifier appleIdTokenVerifier(@Value("${oauth.mobile.apple.client-id:}") String clientId) {
        return new AppleIdTokenVerifier(clientId.trim());
    }

    @Bean
    public AppleClientSecretGenerator appleClientSecretGenerator(
            @Value("${oauth.mobile.apple.client-id:}") String clientId,
            @Value("${oauth.mobile.apple.team-id:${APPLE_TEAM_ID:}}") String teamId,
            @Value("${oauth.mobile.apple.key-id:${APPLE_KEY_ID:}}") String keyId,
            @Value("${oauth.mobile.apple.private-key-path:${APPLE_PRIVATE_KEY_PATH:}}") String privateKeyPath
    ) {
        return new AppleClientSecretGenerator(clientId.trim(), teamId.trim(), keyId.trim(), privateKeyPath.trim());
    }

    @Bean
    public AppleTokenClient appleTokenClient(
            @Value("${oauth.mobile.apple.client-id:}") String clientId, AppleClientSecretGenerator secretGenerator
    ) {
        return new AppleTokenClient(clientId.trim(), secretGenerator);
    }
}
