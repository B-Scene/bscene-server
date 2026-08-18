package com.umc.bscene.global.config;

import com.umc.bscene.global.security.enums.PermitAllUri;
import com.umc.bscene.global.security.filter.JwtAuthFilter;
import com.umc.bscene.global.security.handler.CustomAuthenticationEntryPoint;
import com.umc.bscene.global.security.handler.OAuthFailureHandler;
import com.umc.bscene.global.security.handler.OAuthSuccessHandler;
import com.umc.bscene.global.security.oauth.DynamicRedirectAuthorizationRequestResolver;
import com.umc.bscene.global.security.service.CustomOAuthService;
import com.umc.bscene.global.security.service.CustomOidcUserService;
import com.umc.bscene.global.security.service.CustomUserDetailsService;
import com.umc.bscene.global.security.util.JwtUtil;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuthService customOAuthService;
    private final OAuthSuccessHandler oAuthSuccessHandler;
    private final OAuthFailureHandler oAuthFailureHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final DynamicRedirectAuthorizationRequestResolver dynamicRedirectAuthorizationRequestResolver;

    private final String[] allowUris = Arrays.stream(PermitAllUri.values())
            .map(PermitAllUri::getUri)
            .toArray(String[]::new);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOidcUserService customOidcUserService) throws Exception {
        http
                // CORS 활성화
                .cors(Customizer.withDefaults())

                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // URI 허용 여부
                .authorizeHttpRequests(requests -> requests
                        // SSE 완료 / 에러 콜백 시 발생하는 ASYNC, ERROR Dispatch는 재인가 대상에서 제외 (시청자수 관련 완결 API 콜백)
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(allowUris).permitAll()
                        .anyRequest().authenticated()
                )

                // 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // 소셜 로그인(OAuth2) 설정
                .oauth2Login(oauth -> oauth
                        // 로그인 시작 시 redirect_origin 파라미터를 세션에 기억 (동적 프론트 리다이렉트)
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(dynamicRedirectAuthorizationRequestResolver))
                        // 커스텀 콜백 경로 (application.yaml의 redirect-uri와 일치)
                        .redirectionEndpoint(redirect -> redirect.baseUri("/oauth/callback/*"))
                        // 소셜 유저 정보 로드
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuthService)
                                .oidcUserService(customOidcUserService)
                        )
                        // 로그인 성공 시: 일회성 코드 발급 후 프론트로 리다이렉트
                        .successHandler(oAuthSuccessHandler)
                        // 로그인 실패 시: 프론트로 error 실어 리다이렉트
                        .failureHandler(oAuthFailureHandler)
                )

                // 세션 미사용
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증 예외 응답 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )

                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil, customUserDetailsService);
    }
}