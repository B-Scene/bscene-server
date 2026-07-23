package com.umc.bscene.global.security.handler;

import com.umc.bscene.global.security.oauth.OAuthRedirectOriginSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 소셜 로그인 실패 처리.
 * OAuth 로그인은 브라우저 리다이렉트 흐름이라, 실패도 JSON이 아니라 프론트로 error를 실어 리다이렉트한다.
 * 표준 방식대로 프론트 "로그인 페이지"로 error 코드를 실어 보내고, 프론트가 토스트 등으로 안내한다.
 * (code 교환 단계의 실패는 컨트롤러라 GlobalExceptionHandler가 JSON으로 처리)
 */
@Component
public class OAuthFailureHandler implements AuthenticationFailureHandler {

    private static final String DEFAULT_ERROR = "OAUTH_LOGIN_FAILED";
    private static final String LOGIN_PATH = "/login";

    private final OAuthRedirectOriginSupport redirectOriginSupport;
    private final String frontFailureUri;

    public OAuthFailureHandler(
            OAuthRedirectOriginSupport redirectOriginSupport,
            @Value("${oauth.front-failure-uri}") String frontFailureUri
    ) {
        this.redirectOriginSupport = redirectOriginSupport;
        this.frontFailureUri = frontFailureUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        // CustomOAuthService가 OAuth2AuthenticationException에 담아 던진 에러 코드(OAUTH400_4 등)를 추출
        String error = DEFAULT_ERROR;
        if (exception instanceof OAuth2AuthenticationException oauthException
                && oauthException.getError() != null
                && oauthException.getError().getErrorCode() != null) {
            error = oauthException.getError().getErrorCode();
        }

        String redirectUrl = resolveFailureBase(request) + "?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    // 로그인 시작 시 프론트가 알려준 origin(화이트리스트 검증됨)이 있으면 그쪽 로그인 페이지로, 없으면 기본 주소로
    private String resolveFailureBase(HttpServletRequest request) {
        String origin = redirectOriginSupport.consume(request);
        return (origin != null) ? origin + LOGIN_PATH : frontFailureUri;
    }
}
