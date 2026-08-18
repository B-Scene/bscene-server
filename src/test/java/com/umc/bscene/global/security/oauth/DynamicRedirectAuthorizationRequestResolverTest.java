package com.umc.bscene.global.security.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 소셜 로그인 인가 요청 리졸버 단위테스트.
// 애플만 response_mode=form_post가 추가되는지(애플은 name/email scope 요청 시 form_post 강제),
// 기존 provider와 redirect_origin 저장 동작이 깨지지 않는지가 핵심 분기.
class DynamicRedirectAuthorizationRequestResolverTest {

    private OAuthRedirectOriginSupport redirectOriginSupport;
    private DynamicRedirectAuthorizationRequestResolver resolver;

    @BeforeEach
    void setUp() {
        redirectOriginSupport = new OAuthRedirectOriginSupport(new String[]{"https://bscene.app"});
        resolver = new DynamicRedirectAuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(appleRegistration(), kakaoRegistration()),
                redirectOriginSupport
        );
    }

    private static ClientRegistration appleRegistration() {
        return ClientRegistration.withRegistrationId("apple")
                .clientId("apple-client-id")
                .clientSecret("apple-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://api.bscene.app/api/oauth/callback/apple")
                .scope("openid", "email", "name")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .build();
    }

    private static ClientRegistration kakaoRegistration() {
        return ClientRegistration.withRegistrationId("kakao")
                .clientId("kakao-client-id")
                .clientSecret("kakao-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://api.bscene.app/api/oauth/callback/kakao")
                .scope("profile_nickname", "account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .build();
    }

    private MockHttpServletRequest loginStartRequest(String registrationId) {
        return new MockHttpServletRequest("GET", "/oauth2/authorization/" + registrationId);
    }

    @Test
    void resolve_애플이면_response_mode_form_post를_추가한다() {
        OAuth2AuthorizationRequest request =
                resolver.resolve(loginStartRequest("apple"), "apple");

        assertNotNull(request);
        assertEquals("form_post", request.getAdditionalParameters().get("response_mode"));
        // 실제 애플로 보내는 인가 URL에도 포함되는지까지 확인
        assertTrue(request.getAuthorizationRequestUri().contains("response_mode=form_post"));
    }

    @Test
    void resolve_애플이_아니면_response_mode를_추가하지_않는다() {
        OAuth2AuthorizationRequest request =
                resolver.resolve(loginStartRequest("kakao"), "kakao");

        assertNotNull(request);
        assertFalse(request.getAdditionalParameters().containsKey("response_mode"));
    }

    @Test
    void resolve_인가_시작_경로가_아니면_null을_반환한다() {
        // customize가 delegate의 null 반환(비대상 요청)을 그대로 통과시키는지 — NPE 방지
        assertNull(resolver.resolve(new MockHttpServletRequest("GET", "/api/users/me")));
    }

    @Test
    void resolve_애플_로그인_시작_시에도_redirect_origin은_그대로_저장된다() {
        // form_post 커스터마이징이 기존 동적 리다이렉트 동작을 깨지 않는지 회귀 확인
        MockHttpServletRequest request = loginStartRequest("apple");
        request.setParameter(OAuthRedirectOriginSupport.PARAM_NAME, "https://bscene.app");

        resolver.resolve(request, "apple");

        assertEquals("https://bscene.app", redirectOriginSupport.consume(request));
    }
}
