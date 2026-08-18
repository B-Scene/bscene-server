package com.umc.bscene.global.config;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.format.FormatterRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Part.class, Part::fromValue);
        registry.addConverter(String.class, SkillLevel.class, SkillLevel::fromValue);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 애플 소셜 로그인 콜백은 appleid.apple.com발 cross-origin form_post(POST)라 별도 허용.
        // 먼저 등록해야 아래 /** 매핑보다 우선 매칭된다 (카카오·구글은 GET 리다이렉트라 CORS 검사 미대상)
        registry.addMapping("/oauth/callback/**")
                .allowedOrigins("https://appleid.apple.com")
                .allowedMethods("GET", "POST")
                .allowCredentials(true);

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
//                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowCredentials(true);
    }
}
