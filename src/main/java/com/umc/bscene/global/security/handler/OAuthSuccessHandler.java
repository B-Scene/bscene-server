package com.umc.bscene.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.response.code.BaseResponseCode;
import com.umc.bscene.global.response.code.GeneralSuccessCode;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.entity.OAuthMember;
import com.umc.bscene.global.security.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // 사전 작업: Response 매핑할 ObjectMapper 선언
        ObjectMapper objectMapper = new ObjectMapper();
        BaseResponseCode code = GeneralSuccessCode.SUCCESS_OK;

        // Content-Type, Status 설정
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getStatus());

        // 인증 객체 컨테이너에서 OAuth 인증 객체 가져오기
        OAuthMember member = (OAuthMember) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 토큰 제작을 위해 OAuth 인증 객체에서 User 추출 -> AuthMember 제작
        AuthMember authMember = new AuthMember(member.getUser());
        String accessToken = jwtUtil.createAccessToken(authMember);
        String refreshToken = jwtUtil.createRefreshToken(authMember);
        // TODO: refreshToken 저장소(DB) 연동 필요 (현재는 클라이언트에만 전달)

        // 응답 통일 객체 래핑 (TODO: 전용 로그인 응답 DTO로 교체 + refresh 저장소(DB) 연동)
        SuccessResponse<Map<String, String>> responseBody = SuccessResponse.of(
                Map.of("accessToken", accessToken, "refreshToken", refreshToken),
                code
        );

        // 응답 출력
        objectMapper.writeValue(response.getOutputStream(), responseBody);
    }
}