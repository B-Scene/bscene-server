package com.umc.bscene.global.security.handler;

import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.global.security.entity.OAuthMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final String frontRedirectUri;

    public OAuthSuccessHandler(
            OauthService oauthService,
            @Value("${oauth.front-redirect-uri}") String frontRedirectUri
    ) {
        this.oauthService = oauthService;
        this.frontRedirectUri = frontRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        // 소셜 로그인 결과 처리 (기존 유저 → 토큰 / 신규 유저 → signup 토큰)
        OAuthMember member = (OAuthMember) authentication.getPrincipal();
        OauthLoginResponse result = oauthService.handleLoginSuccess(member);

        // 결과를 일회성 코드에 담아두고, 프론트로는 code만 실어 리다이렉트(토큰 URL 노출 방지)
        String code = oauthService.issueExchangeCode(result);
        String redirectUrl = frontRedirectUri + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
