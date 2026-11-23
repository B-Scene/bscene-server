package com.umc.bscene.global.security.handler;

import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.oauth.OAuthRedirectOriginSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Slf4j
@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final String CALLBACK_PATH = "/oauth/callback";
    private static final String LOGIN_PATH = "/login";
    // 비즈니스 예외가 아닌 예기치 못한 오류용 코드 (OAuthFailureHandler 의 기본값과 동일)
    private static final String DEFAULT_ERROR = "OAUTH_LOGIN_FAILED";

    private final OauthService oauthService;
    private final OAuthRedirectOriginSupport redirectOriginSupport;
    private final String frontRedirectUri;
    private final String frontFailureUri;

    public OAuthSuccessHandler(
            OauthService oauthService,
            OAuthRedirectOriginSupport redirectOriginSupport,
            @Value("${oauth.front-redirect-uri}") String frontRedirectUri,
            @Value("${oauth.front-failure-uri}") String frontFailureUri
    ) {
        this.oauthService = oauthService;
        this.redirectOriginSupport = redirectOriginSupport;
        this.frontRedirectUri = frontRedirectUri;
        this.frontFailureUri = frontFailureUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        // origin 은 세션에서 1회만 꺼낼 수 있으므로 성공/실패 분기 전에 먼저 확보
        String origin = redirectOriginSupport.consume(request);
        OAuthMember member = (OAuthMember) authentication.getPrincipal();

        String code;
        try {
            // 소셜 로그인 결과 처리 (기존 유저 → 토큰 / 신규 유저 → signup 토큰)
            OauthLoginResponse result = oauthService.handleLoginSuccess(member);
            // 결과를 일회성 코드에 담아두고, 프론트로는 code만 실어 리다이렉트(토큰 URL 노출 방지)
            code = oauthService.issueExchangeCode(result);
        } catch (BaseException e) {
            // 정지·휴면·탈퇴 거절(AUTH403_*) 같은 비즈니스 예외.
            // 리다이렉트 흐름이라 GlobalExceptionHandler 가 못 잡으므로 실패 핸들러와 같은 방식으로 프론트 로그인 페이지에 error 코드를 전달
            redirectToFailure(response, origin, e.getBaseResponseCode().getCode());
            return;
        } catch (RuntimeException e) {
            // Redis 장애 등 예기치 못한 오류. 에러 페이지 대신 프론트가 안내할 수 있게 기본 코드로 보낸다
            log.error("소셜 로그인 성공 처리 중 오류", e);
            redirectToFailure(response, origin, DEFAULT_ERROR);
            return;
        }

        String redirectUrl = resolveRedirectBase(origin) + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    private void redirectToFailure(HttpServletResponse response, String origin, String error) throws IOException {
        String redirectUrl = resolveFailureBase(origin) + "?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    // 로그인 시작 시 프론트가 알려준 origin(화이트리스트 검증됨)이 있으면 그리로, 없으면 기본 프론트 주소로
    private String resolveRedirectBase(String origin) {
        return (origin != null) ? origin + CALLBACK_PATH : frontRedirectUri;
    }

    private String resolveFailureBase(String origin) {
        return (origin != null) ? origin + LOGIN_PATH : frontFailureUri;
    }
}
