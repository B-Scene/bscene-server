package com.umc.bscene.global.config;

import com.umc.bscene.global.security.oauth.kakao.KakaoAccessTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class KakaoMobileOauthConfig {

    @Bean
    public KakaoAccessTokenVerifier kakaoAccessTokenVerifier(@Value("${oauth.mobile.kakao.app-id:}") String appId) {
        return new KakaoAccessTokenVerifier(createKakaoRestClient(), appId.trim());
    }

    // 로그인 요청 스레드에서 카카오를 두 번 호출하므로 타임아웃 필수 (StreamConfig.mtxRestClient 와 같은 값).
    // 타임아웃은 ResourceAccessException 으로 던져져 검증기에서 OAUTH503_1 로 변환된다.
    // RestClient 빈으로 노출하지 않는 이유: mtxRestClient 와 타입이 겹쳐 주입 모호성이 생기지 않게 하려고.
    private static RestClient createKakaoRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
