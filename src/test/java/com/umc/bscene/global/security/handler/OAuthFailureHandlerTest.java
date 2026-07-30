package com.umc.bscene.global.security.handler;

import com.umc.bscene.global.security.oauth.OAuthRedirectOriginSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

// 소셜 로그인 실패 핸들러 단위테스트.
// 검증 대상 : consume된 origin 유무에 따른 로그인 페이지 분기 + 에러 코드 추출/기본값.
@ExtendWith(MockitoExtension.class)
class OAuthFailureHandlerTest {

    private static final String FRONT_FAILURE_URI = "https://bscene.app/login";

    @Mock
    private OAuthRedirectOriginSupport redirectOriginSupport;

    private OAuthFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuthFailureHandler(redirectOriginSupport, FRONT_FAILURE_URI);
    }

    @Test
    void 저장된_redirect_origin이_없으면_기본_로그인_페이지로_리다이렉트한다() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn(null);
        AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("OAUTH400_4"));

        handler.onAuthenticationFailure(request, response, exception);

        assertEquals(FRONT_FAILURE_URI + "?error=OAUTH400_4", response.getRedirectedUrl());
    }

    @Test
    void 저장된_redirect_origin이_있으면_그쪽_로그인_페이지로_리다이렉트한다() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn("http://localhost:5173");
        AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("OAUTH400_4"));

        handler.onAuthenticationFailure(request, response, exception);

        assertEquals("http://localhost:5173/login?error=OAUTH400_4", response.getRedirectedUrl());
    }

    @Test
    void OAuth2_예외가_아니면_기본_에러_코드로_리다이렉트한다() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(redirectOriginSupport.consume(request)).thenReturn(null);
        // 에러 코드를 담지 않은 일반 인증 예외
        AuthenticationException exception = new AuthenticationException("인증 실패") {
        };

        handler.onAuthenticationFailure(request, response, exception);

        assertEquals(FRONT_FAILURE_URI + "?error=OAUTH_LOGIN_FAILED", response.getRedirectedUrl());
    }
}
