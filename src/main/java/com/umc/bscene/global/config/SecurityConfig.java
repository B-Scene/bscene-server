package com.umc.bscene.global.config;

import com.umc.bscene.global.security.enums.PermitAllUri;
import com.umc.bscene.global.security.filter.JwtAuthFilter;
import com.umc.bscene.global.security.handler.OAuthSuccessHandler;
import com.umc.bscene.global.security.service.CustomOAuthService;
import com.umc.bscene.global.security.service.CustomUserDetailsService;
import com.umc.bscene.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private final String[] allowUris = Arrays.stream(PermitAllUri.values())
            .map(PermitAllUri::getUri)
            .toArray(String[]::new);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // URI 허용 여부
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(allowUris).permitAll()
                        .anyRequest().authenticated()
                )

                // 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // 소셜 로그인(OAuth2) 설정
                .oauth2Login(oauth -> oauth
                        // 커스텀 콜백 경로 (application.yaml의 redirect-uri와 일치)
                        .redirectionEndpoint(redirect -> redirect.baseUri("/oauth/callback/*"))
                        // 소셜 유저 정보 로드
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuthService))
                        // 로그인 성공 시 JWT 발급 핸들러
                        .successHandler(oAuthSuccessHandler)
                )

                // 세션 미사용
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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