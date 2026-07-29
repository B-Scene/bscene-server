package com.umc.bscene.global.security.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.security.enums.PermitAllUri;
import com.umc.bscene.global.security.service.CustomUserDetailsService;
import com.umc.bscene.global.security.util.JwtUtil;
import com.umc.bscene.support.StreamFixtures;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * JwtAuthFilter 단위 테스트.
 * <p>
 * Spring 컨텍스트 없이 doFilterInternal을 직접 호출하고, 서블릿 목은 spring-test의
 * MockHttpServletRequest/Response를 쓴다. 체인을 삼키는 필터는 곧 장애이므로
 * 모든 분기에서 doFilter 호출 횟수를 명시적으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    private static final String VALID_TOKEN = "valid.access.token";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil, customUserDetailsService);
        request = new MockHttpServletRequest("GET", "/lives");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        // 컨텍스트가 다음 테스트로 새지 않도록 반드시 비운다.
        SecurityContextHolder.clearContext();
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void givenAuthenticatedUser(String token, Long userId) {
        given(jwtUtil.isValid(token)).willReturn(true);
        given(jwtUtil.getType(token)).willReturn("access");
        given(jwtUtil.getUserId(token)).willReturn(String.valueOf(userId));
        given(customUserDetailsService.loadUserByUsername(String.valueOf(userId)))
                .willReturn(new AuthMember(StreamFixtures.bandUser(userId)));
    }

    @Nested
    @DisplayName("토큰이 없는 요청")
    class WithoutToken {

        @Test
        @DisplayName("Authorization 헤더가 없으면 인증 없이 체인만 한 번 진행한다")
        void continuesWithoutAuthentication() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtUtil, customUserDetailsService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "abc.def.ghi", "Basic dXNlcjpwYXNz", "bearer lower-case-prefix", "Bearer"})
        @DisplayName("Bearer 접두어가 없는 헤더는 토큰으로 보지 않고 인증 없이 체인만 진행한다")
        void ignoresMalformedAuthorizationHeader(String header) throws Exception {
            request.addHeader("Authorization", header);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtUtil, customUserDetailsService);
        }

        @Test
        @DisplayName("access_token 이외의 쿠키만 있으면 인증 없이 체인만 진행한다")
        void ignoresUnrelatedCookie() throws Exception {
            request.setCookies(new Cookie("refresh_token", "some-refresh-token"));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtUtil, customUserDetailsService);
        }

        @Test
        @DisplayName("permitAll 대상 URI도 필터에서는 동일하게 통과시킨다 (화이트리스트는 SecurityConfig 책임)")
        void passesThroughPermitAllUri() throws Exception {
            request = new MockHttpServletRequest("POST", PermitAllUri.AUTH_LOGIN.getUri());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtUtil, customUserDetailsService);
        }
    }

    @Nested
    @DisplayName("유효한 access 토큰")
    class WithValidAccessToken {

        @Test
        @DisplayName("Bearer 헤더의 토큰으로 SecurityContext에 인증 객체를 등록한다")
        void authenticatesFromBearerHeader() throws Exception {
            givenAuthenticatedUser(VALID_TOKEN, 7L);
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            filter.doFilterInternal(request, response, filterChain);

            Authentication authentication = currentAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getPrincipal()).isInstanceOf(AuthMember.class);
            assertThat(((AuthMember) authentication.getPrincipal()).getUser().getId()).isEqualTo(7L);
            assertThat(authentication.getName()).isEqualTo("7");
            assertThat(authentication.getCredentials()).isNull();
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_BAND");
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("access_token 쿠키로도 동일하게 인증된다")
        void authenticatesFromCookie() throws Exception {
            givenAuthenticatedUser(VALID_TOKEN, 11L);
            request.setCookies(new Cookie("access_token", VALID_TOKEN));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNotNull();
            assertThat(currentAuthentication().getName()).isEqualTo("11");
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Authorization 헤더가 쿠키보다 우선한다")
        void prefersHeaderOverCookie() throws Exception {
            givenAuthenticatedUser(VALID_TOKEN, 3L);
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            request.setCookies(new Cookie("access_token", "cookie.token.value"));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNotNull();
            assertThat(currentAuthentication().getName()).isEqualTo("3");
            verify(jwtUtil).isValid(VALID_TOKEN);
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("거부되는 토큰")
    class WithRejectedToken {

        @Test
        @DisplayName("서명이 깨졌거나 만료된 토큰은 인증 없이 체인만 진행한다")
        void invalidTokenIsIgnored() throws Exception {
            given(jwtUtil.isValid("expired.token")).willReturn(false);
            request.addHeader("Authorization", "Bearer expired.token");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(200);
            verify(jwtUtil, never()).getType(anyString());
            verifyNoInteractions(customUserDetailsService);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("type이 access가 아닌 refresh 토큰은 인증 없이 체인만 진행한다")
        void refreshTokenIsIgnored() throws Exception {
            given(jwtUtil.isValid("refresh.token")).willReturn(true);
            given(jwtUtil.getType("refresh.token")).willReturn("refresh");
            request.addHeader("Authorization", "Bearer refresh.token");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            verify(jwtUtil, never()).getUserId(anyString());
            verifyNoInteractions(customUserDetailsService);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("type이 null인 토큰도 인증되지 않는다")
        void nullTypeIsIgnored() throws Exception {
            given(jwtUtil.isValid("no.type.token")).willReturn(true);
            given(jwtUtil.getType("no.type.token")).willReturn(null);
            request.addHeader("Authorization", "Bearer no.type.token");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            verifyNoInteractions(customUserDetailsService);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("\"Bearer \"만 있어 토큰이 빈 문자열이어도 예외 없이 체인만 진행한다")
        void emptyBearerTokenIsIgnored() throws Exception {
            given(jwtUtil.isValid("")).willReturn(false);
            request.addHeader("Authorization", "Bearer ");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("토큰 처리 중 예외가 난 경우")
    class WhenTokenProcessingThrows {

        @Test
        @DisplayName("존재하지 않는 회원이면 401 JSON을 쓰고 체인을 진행하지 않는다")
        void writesUnauthorizedAndStopsChain() throws Exception {
            given(jwtUtil.isValid(VALID_TOKEN)).willReturn(true);
            given(jwtUtil.getType(VALID_TOKEN)).willReturn("access");
            given(jwtUtil.getUserId(VALID_TOKEN)).willReturn("999");
            given(customUserDetailsService.loadUserByUsername("999"))
                    .willThrow(new UsernameNotFoundException("존재하지 않는 회원입니다. userId=999"));
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(currentAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(GeneralErrorCode.UNAUTHORIZED_ERROR.getStatus());
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
            assertThat(body.get("isSuccess").asBoolean()).isFalse();
            assertThat(body.get("status").asInt()).isEqualTo(401);
            assertThat(body.get("code").asText()).isEqualTo(GeneralErrorCode.UNAUTHORIZED_ERROR.getCode());
            assertThat(body.get("message").asText()).isEqualTo(GeneralErrorCode.UNAUTHORIZED_ERROR.getMessage());

            // 401을 이미 썼으므로 체인은 진행하지 않는다.
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("토큰 검증 자체가 터져도 필터 밖으로 예외를 던지지 않는다")
        void doesNotPropagateException() {
            given(jwtUtil.isValid("boom")).willThrow(new IllegalStateException("jwt parser blew up"));
            request.addHeader("Authorization", "Bearer boom");

            assertThatCode(() -> filter.doFilterInternal(request, response, filterChain))
                    .doesNotThrowAnyException();

            assertThat(currentAuthentication()).isNull();
            assertThat(response.getStatus()).isEqualTo(401);
        }
    }
}
