package com.umc.bscene.global.security.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.enums.PermitAllUri;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * RateLimitFilter 단위 테스트.
 * <p>
 * Spring 컨텍스트 없이 OncePerRequestFilter.doFilter 를 직접 호출해 shouldNotFilter 분기까지 검증한다.
 * Redis 는 목으로 두고 Lua 스크립트 결과 {count, ttl} 만 흉내 낸다.
 * 체인을 삼키는 필터는 곧 장애이므로 모든 분기에서 doFilter 호출 횟수를 명시적으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    private static final int LOGIN_LIMIT = 30;
    private static final int CHALLENGE_LIMIT = 10;
    private static final String CLIENT_IP = "203.0.113.5";
    private static final String WINDOW_SECONDS = "60";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(redis, LOGIN_LIMIT, CHALLENGE_LIMIT);
        request = new MockHttpServletRequest("POST", PermitAllUri.OAUTH_MOBILE_APPLE_CHALLENGE.getUri());
        request.setRemoteAddr("10.0.0.1");
        request.addHeader(RateLimitFilter.CLIENT_IP_HEADER, CLIENT_IP);
        response = new MockHttpServletResponse();
    }

    @SuppressWarnings("unchecked")
    private void givenCounter(long count, long ttl) {
        given(redis.execute(any(RedisScript.class), anyList(), any())).willReturn(List.of(count, ttl));
    }

    @SuppressWarnings("unchecked")
    private String capturedKey() {
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redis).execute(any(RedisScript.class), keys.capture(), eq(WINDOW_SECONDS));
        assertThat(keys.getValue()).hasSize(1);
        return keys.getValue().get(0);
    }

    private JsonNode responseBody() throws Exception {
        return new ObjectMapper().readTree(response.getContentAsString());
    }

    @Nested
    @DisplayName("한도 설정 검증")
    class LimitConfiguration {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("login 한도가 1 미만이면 생성 시점에 실패한다 (전부 429 가 되는 설정 오류를 기동 시 차단)")
        void rejectsNonPositiveLoginLimit(int limit) {
            assertThatThrownBy(() -> new RateLimitFilter(redis, limit, CHALLENGE_LIMIT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("login-per-minute")
                    .hasMessageContaining(String.valueOf(limit));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        @DisplayName("challenge 한도가 1 미만이면 생성 시점에 실패한다")
        void rejectsNonPositiveChallengeLimit(int limit) {
            assertThatThrownBy(() -> new RateLimitFilter(redis, LOGIN_LIMIT, limit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("challenge-per-minute");
        }

        @Test
        @DisplayName("한도가 1 이면 정상 생성된다")
        void acceptsMinimumLimit() {
            assertThatCode(() -> new RateLimitFilter(redis, 1, 1)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("한도 이내")
    class WithinLimit {

        @Test
        @DisplayName("카운트가 한도와 같을 때까지는 체인을 한 번 진행하고 Retry-After 를 붙이지 않는다")
        void passesThroughAtLimit() throws Exception {
            givenCounter(CHALLENGE_LIMIT, 55);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader(RateLimitFilter.RETRY_AFTER_HEADER)).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("challenge 경로는 challenge 버킷 키로 X-Real-IP 기준 카운트한다")
        void countsChallengeBucketByRealIp() throws Exception {
            givenCounter(1, 60);

            filter.doFilter(request, response, filterChain);

            assertThat(capturedKey()).isEqualTo(RateLimitFilter.KEY_PREFIX + "challenge:" + CLIENT_IP);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/auth/oauth/mobile/kakao", "/auth/oauth/mobile/apple"})
        @DisplayName("로그인 2개 경로는 같은 login 버킷을 공유한다")
        void loginPathsShareLoginBucket(String path) throws Exception {
            request = new MockHttpServletRequest("POST", path);
            request.addHeader(RateLimitFilter.CLIENT_IP_HEADER, CLIENT_IP);
            givenCounter(LOGIN_LIMIT, 30);

            filter.doFilter(request, response, filterChain);

            assertThat(capturedKey()).isEqualTo(RateLimitFilter.KEY_PREFIX + "login:" + CLIENT_IP);
            assertThat(response.getStatus()).isEqualTo(200);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("context-path(/api)가 붙은 요청 URI 도 대상 경로로 인식한다")
        void matchesPathUnderContextPath() throws Exception {
            request = new MockHttpServletRequest("POST", "/api" + PermitAllUri.OAUTH_MOBILE_KAKAO.getUri());
            request.setContextPath("/api");
            request.addHeader(RateLimitFilter.CLIENT_IP_HEADER, CLIENT_IP);
            givenCounter(1, 60);

            filter.doFilter(request, response, filterChain);

            assertThat(capturedKey()).isEqualTo(RateLimitFilter.KEY_PREFIX + "login:" + CLIENT_IP);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("퍼센트 인코딩된 경로(/mobile/%6Bakao)도 디코딩해서 같은 버킷으로 카운트한다")
        void matchesPercentEncodedPath() throws Exception {
            // Spring MVC 는 디코딩해서 /kakao 컨트롤러로 보내므로, 필터가 원문 URI 로 비교하면 한도 없이 통과하는 우회가 된다
            request = new MockHttpServletRequest("POST", "/auth/oauth/mobile/%6Bakao");
            request.addHeader(RateLimitFilter.CLIENT_IP_HEADER, CLIENT_IP);
            givenCounter(LOGIN_LIMIT + 1, 10);

            filter.doFilter(request, response, filterChain);

            assertThat(capturedKey()).isEqualTo(RateLimitFilter.KEY_PREFIX + "login:" + CLIENT_IP);
            assertThat(response.getStatus()).isEqualTo(429);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("X-Real-IP 가 없으면 remoteAddr 로 카운트한다")
        void fallsBackToRemoteAddr() throws Exception {
            request = new MockHttpServletRequest("POST", PermitAllUri.OAUTH_MOBILE_APPLE_CHALLENGE.getUri());
            request.setRemoteAddr("10.0.0.1");
            givenCounter(1, 60);

            filter.doFilter(request, response, filterChain);

            assertThat(capturedKey()).isEqualTo(RateLimitFilter.KEY_PREFIX + "challenge:10.0.0.1");
        }

        @Test
        @DisplayName("X-Real-IP 가 공백이면 remoteAddr 로 카운트한다")
        void treatsBlankRealIpAsMissing() throws Exception {
            request = new MockHttpServletRequest("POST", PermitAllUri.OAUTH_MOBILE_APPLE_CHALLENGE.getUri());
            request.setRemoteAddr("10.0.0.1");
            request.addHeader(RateLimitFilter.CLIENT_IP_HEADER, "   ");
            givenCounter(1, 60);

            filter.doFilter(request, response, filterChain);

            assertThat(capturedKey()).isEqualTo(RateLimitFilter.KEY_PREFIX + "challenge:10.0.0.1");
        }
    }

    @Nested
    @DisplayName("한도 초과")
    class OverLimit {

        @Test
        @DisplayName("한도를 넘으면 429 JSON 과 남은 초 Retry-After 를 쓰고 체인을 진행하지 않는다")
        void writesTooManyRequestsAndStopsChain() throws Exception {
            givenCounter(CHALLENGE_LIMIT + 1, 42);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(OauthErrorCode.TOO_MANY_LOGIN_ATTEMPTS.getStatus());
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
            assertThat(response.getHeader(RateLimitFilter.RETRY_AFTER_HEADER)).isEqualTo("42");

            JsonNode body = responseBody();
            assertThat(body.get("isSuccess").asBoolean()).isFalse();
            assertThat(body.get("status").asInt()).isEqualTo(429);
            assertThat(body.get("code").asText()).isEqualTo(OauthErrorCode.TOO_MANY_LOGIN_ATTEMPTS.getCode());
            assertThat(body.get("message").asText()).isEqualTo(OauthErrorCode.TOO_MANY_LOGIN_ATTEMPTS.getMessage());

            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("login 버킷은 30회를 넘긴 31번째부터 429 다")
        void loginBucketRejectsBeyondThirty() throws Exception {
            request = new MockHttpServletRequest("POST", PermitAllUri.OAUTH_MOBILE_KAKAO.getUri());
            request.addHeader(RateLimitFilter.CLIENT_IP_HEADER, CLIENT_IP);
            givenCounter(LOGIN_LIMIT + 1, 10);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("남은 TTL 이 0 이어도 Retry-After 는 최소 1초다")
        void retryAfterIsAtLeastOneSecond() throws Exception {
            givenCounter(CHALLENGE_LIMIT + 1, 0);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader(RateLimitFilter.RETRY_AFTER_HEADER)).isEqualTo("1");
        }
    }

    @Nested
    @DisplayName("Redis 장애")
    class WhenRedisFails {

        @Test
        @DisplayName("Redis 연결 예외면 우회 통과시키지 않고 503 JSON 을 쓴다")
        void writesServiceUnavailableOnConnectionFailure() throws Exception {
            given(redis.execute(any(RedisScript.class), anyList(), any()))
                    .willThrow(new RedisConnectionFailureException("redis down"));

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(GeneralErrorCode.STORE_UNAVAILABLE.getStatus());
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            JsonNode body = responseBody();
            assertThat(body.get("status").asInt()).isEqualTo(503);
            assertThat(body.get("code").asText()).isEqualTo(GeneralErrorCode.STORE_UNAVAILABLE.getCode());

            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("스크립트 결과가 null 이면 503 으로 막는다")
        void writesServiceUnavailableOnNullResult() throws Exception {
            given(redis.execute(any(RedisScript.class), anyList(), any())).willReturn(null);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(503);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("스크립트 결과가 {count, ttl} 두 개가 아니면 503 으로 막는다")
        void writesServiceUnavailableOnMalformedResult() throws Exception {
            // Lua 반환 형태가 바뀌어 count 만 오는 경우. 조용히 통과시키지 않고 막아야 테스트로 잡힌다
            given(redis.execute(any(RedisScript.class), anyList(), any())).willReturn(List.of(1L));

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(responseBody().get("code").asText()).isEqualTo(GeneralErrorCode.STORE_UNAVAILABLE.getCode());
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("대상이 아닌 요청")
    class NotTargeted {

        @Test
        @DisplayName("모바일 로그인 경로가 아니면 Redis 를 건드리지 않고 체인만 진행한다")
        void skipsOtherPaths() throws Exception {
            request = new MockHttpServletRequest("POST", PermitAllUri.AUTH_LOGIN.getUri());

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(redis);
        }

        @Test
        @DisplayName("대상 경로라도 POST 가 아니면 카운트하지 않는다")
        void skipsNonPostMethods() throws Exception {
            request = new MockHttpServletRequest("GET", PermitAllUri.OAUTH_MOBILE_APPLE_CHALLENGE.getUri());

            filter.doFilter(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(redis);
        }

        @Test
        @DisplayName("대상 경로 아래의 다른 하위 경로는 대상이 아니다")
        void skipsSimilarButDifferentPaths() throws Exception {
            request = new MockHttpServletRequest("POST", PermitAllUri.OAUTH_MOBILE_KAKAO.getUri() + "/extra");

            filter.doFilter(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(redis);
        }
    }
}
