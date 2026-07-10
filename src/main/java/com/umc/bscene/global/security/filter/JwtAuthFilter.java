package com.umc.bscene.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.service.CustomUserDetailsService;
import com.umc.bscene.global.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
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

        String token = getToken(request);

        if (token != null) {
            // 토큰 처리 예외만 401로 변환한다.
            // filterChain.doFilter를 try 안에 두면 다운스트림(컨트롤러 등) 예외까지
            // 전부 401로 둔갑하므로, catch 범위를 토큰 검증부로 한정한다.
            try {
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
            } catch (Exception e) {
                ObjectMapper mapper = new ObjectMapper();
                GeneralErrorCode code = GeneralErrorCode.UNAUTHORIZED_ERROR;

                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(code.getStatus());

                ErrorResponse<Void> errorResponse = ErrorResponse.from(code);

                mapper.writeValue(response.getOutputStream(), errorResponse);
                return; // 401 응답을 썼으므로 체인 진행 중단
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer "))
            return header.substring(7);

        if (request.getCookies() != null)
            for (Cookie cookie : request.getCookies())
                if ("access_token".equals(cookie.getName())) return cookie.getValue();

        return null;
    }
}
