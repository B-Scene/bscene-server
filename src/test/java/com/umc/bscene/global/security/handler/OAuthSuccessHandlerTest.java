package com.umc.bscene.global.security.handler;

import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.global.security.dto.response.OAuthResponse;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.oauth.OAuthRedirectOriginSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// 소셜 로그인 성공 핸들러 단위테스트.
// 검증 대상 : consume된 origin 유무에 따른 리다이렉트 목적지 분기 + 일회성 code 파라미터 부착.
@ExtendWith(MockitoExtension.class)
class OAuthSuccessHandlerTest {

    private static final String FRONT_REDIRECT_URI = "https://bscene.app/oauth/callback";

    @Mock
    private OauthService oauthService;
    @Mock
    private OAuthRedirectOriginSupport redirectOriginSupport;
    @Mock
    private Authentication authentication;
    @Mock
    private OAuthResponse oauthInfo;

    private OAuthSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuthSuccessHandler(oauthService, redirectOriginSupport, FRONT_REDIRECT_URI);
    }

    private void mockLoginSuccess(String code) {
        OAuthMember member = OAuthMember.ofNew(oauthInfo, Map.of());
        when(authentication.getPrincipal()).thenReturn(member);
        OauthLoginResponse result = OauthLoginResponse.ofNew("signup-token", "sejin@kakao.com");
        when(oauthService.handleLoginSuccess(member)).thenReturn(result);
        when(oauthService.issueExchangeCode(result)).thenReturn(code);
    }

    @Test
    void 저장된_redirect_origin이_없으면_기본_프론트_주소로_리다이렉트한다() throws IOException {
        mockLoginSuccess("code-123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(FRONT_REDIRECT_URI + "?code=code-123", response.getRedirectedUrl());
    }

    @Test
    void 저장된_redirect_origin이_있으면_그쪽_콜백_경로로_리다이렉트한다() throws IOException {
        mockLoginSuccess("code-123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 로컬 프론트에서 시작한 로그인 → 화이트리스트 통과 후 세션에 저장됐던 origin
        when(redirectOriginSupport.consume(request)).thenReturn("http://localhost:5173");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("http://localhost:5173/oauth/callback?code=code-123", response.getRedirectedUrl());
    }

    @Test
    void 교환_코드는_URL_인코딩되어_부착된다() throws IOException {
        // UUID 코드는 원래 URL-safe지만, 인코딩 경로 자체가 동작하는지 특수문자로 확인
        mockLoginSuccess("a b&c");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(FRONT_REDIRECT_URI + "?code=a+b%26c", response.getRedirectedUrl());
    }

    @Test
    void 토큰이_아니라_일회성_코드만_URL에_실린다() throws IOException {
        mockLoginSuccess("code-123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(any())).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        // 토큰 URL 노출 방지 설계 확인 — signup-token이 리다이렉트 URL에 등장하면 안 됨
        assertEquals(false, response.getRedirectedUrl().contains("signup-token"));
    }
}
