package com.umc.bscene.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.oauth.dto.response.OauthLoginResponse;
import com.umc.bscene.domain.oauth.response.code.OauthSuccessCode;
import com.umc.bscene.domain.oauth.service.OauthService;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.OAuthMember;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // 인증 객체에서 소셜 로그인 결과 처리 (기존 유저 → 토큰 / 신규 유저 → signup 토큰)
        OAuthMember member = (OAuthMember) authentication.getPrincipal();
        OauthLoginResponse result = oauthService.handleLoginSuccess(member);

        SuccessResponse<OauthLoginResponse> body = SuccessResponse.of(
                result,
                OauthSuccessCode.OAUTH_LOGIN_SUCCESS
        );

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(body.getStatus());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
