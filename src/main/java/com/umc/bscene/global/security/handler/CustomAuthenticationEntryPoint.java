package com.umc.bscene.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 인증되지 않은 사용자가 인증이 필요한 API에 접근했을 때 JSON 응답을 반환합니다.
    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authException
    ) throws IOException {
        GeneralErrorCode code = GeneralErrorCode.UNAUTHORIZED_ERROR;
        ErrorResponse<Void> errorResponse = ErrorResponse.from(code);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getStatus());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}