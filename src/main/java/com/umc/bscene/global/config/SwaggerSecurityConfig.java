package com.umc.bscene.global.config;

import com.umc.bscene.global.security.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SwaggerSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain swaggerSecurityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            PasswordEncoder passwordEncoder,
            @Value("${swagger.admin.token:}") String swaggerToken
    ) throws Exception {
        InMemoryUserDetailsManager accounts = new InMemoryUserDetailsManager();
        if (!swaggerToken.isBlank()) {
            accounts.createUser(User.withUsername("bscene-admin")
                    .password(passwordEncoder.encode(swaggerToken))
                    .roles("ADMIN")
                    .build());
        }
        DaoAuthenticationProvider swaggerAuthentication = new DaoAuthenticationProvider(accounts);
        swaggerAuthentication.setPasswordEncoder(passwordEncoder);

        return http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs",
                        "/v3/api-docs/**", "/v3/api-docs.yaml", "/v3/api-docs.yaml/**",
                        "/webjars/swagger-ui/**")
                .authenticationProvider(swaggerAuthentication)
                .authorizeHttpRequests(requests -> requests.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .addFilterAfter(jwtAuthFilter, BasicAuthenticationFilter.class)
                .build();
    }
}
