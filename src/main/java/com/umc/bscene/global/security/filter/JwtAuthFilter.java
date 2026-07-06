package com.umc.bscene.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.BaseResponseCode;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.exception.JwtAuthenticationException;
import com.umc.bscene.global.security.service.CustomUserDetailsService;
import com.umc.bscene.global.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 토큰 가져오기
            String token = request.getHeader("Authorization");

            // token이 없거나 Bearer가 아니면 넘기기
            if (token == null || !token.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Bearer이면 토큰만 추출
            token = token.replace("Bearer ", "");

            // AccessToken 검증하기
            if (jwtUtil.isValid(token) && "access".equals(jwtUtil.getType(token))) {
                // 토큰에서 userId 추출
                String userId = jwtUtil.getUserId(token);

                // userId로 회원 조회 후 인증 객체 생성
                UserDetails user = customUserDetailsService.loadUserByUsername(userId);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

                // 인증 완료 후 SecurityContextHolder에 등록
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException e) {
            writeErrorResponse(response, e.getBaseResponseCode());
        } catch (Exception e) {
            writeErrorResponse(response, GeneralErrorCode.UNAUTHORIZED_ERROR);
        }
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            BaseResponseCode code
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ErrorResponse<Void> errorResponse = ErrorResponse.from(code);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getStatus());

        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}