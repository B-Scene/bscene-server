package com.umc.bscene.global.security.handler;

import com.umc.bscene.domain.auth.enums.code.AuthErrorCode;
import com.umc.bscene.domain.auth.exception.auth.AuthException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 소셜 로그인 성공 핸들러 단위테스트.
// 검증 대상 : consume된 origin 유무에 따른 리다이렉트 목적지 분기 + 일회성 code 파라미터 부착
//           + 로그인 거절(계정 상태 등)·예기치 못한 오류 시 실패 페이지 리다이렉트.
@ExtendWith(MockitoExtension.class)
class OAuthSuccessHandlerTest {

    private static final String FRONT_REDIRECT_URI = "https://bscene.app/oauth/callback";
    private static final String FRONT_FAILURE_URI = "https://bscene.app/login";

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
        handler = new OAuthSuccessHandler(oauthService, redirectOriginSupport, FRONT_REDIRECT_URI, FRONT_FAILURE_URI);
    }

    private OAuthMember mockLoginFailure(RuntimeException exception) {
        OAuthMember member = OAuthMember.ofNew(oauthInfo, Map.of());
        when(authentication.getPrincipal()).thenReturn(member);
        when(oauthService.handleLoginSuccess(member)).thenThrow(exception);
        return member;
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

    // ---------- 로그인 거절 / 오류 → 실패 페이지 리다이렉트 ----------

    @Test
    void 정지_계정이면_실패_페이지로_에러_코드를_실어_리다이렉트한다() throws IOException {
        mockLoginFailure(new AuthException(AuthErrorCode.SUSPENDED_ACCOUNT));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(FRONT_FAILURE_URI + "?error=AUTH403_1", response.getRedirectedUrl());
        // 거절된 로그인은 교환 코드를 만들지 않는다
        verify(oauthService, never()).issueExchangeCode(any());
    }

    @Test
    void 거절_시에도_저장된_redirect_origin이_있으면_그쪽_로그인_페이지로_보낸다() throws IOException {
        mockLoginFailure(new AuthException(AuthErrorCode.DELETED_ACCOUNT));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn("http://localhost:5173");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("http://localhost:5173/login?error=AUTH403_3", response.getRedirectedUrl());
    }

    @Test
    void 예기치_못한_오류면_기본_에러_코드로_실패_페이지에_보낸다() throws IOException {
        // Redis 장애 등 비즈니스 예외가 아닌 경우 — 에러 페이지가 아니라 프론트가 안내할 수 있어야 함
        mockLoginFailure(new IllegalStateException("redis down"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(FRONT_FAILURE_URI + "?error=OAUTH_LOGIN_FAILED", response.getRedirectedUrl());
    }
}
