package com.umc.bscene.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.BaseResponseCode;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.global.security.enums.PermitAllUri;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

// 모바일 SDK 소셜 로그인 4개 경로의 IP당 분당 호출 제한 (명세 SDK v0.2, 출시 필수).
// 로그인 전 호출이라 JwtAuthFilter 앞에서 동작하고, ControllerAdvice 밖이므로 응답 JSON을 직접 쓴다.
// 카운트는 Redis INCR + EXPIRE(60초) 고정 윈도우. Redis 장애 시 우회 통과시키지 않고 503으로 응답한다.
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final String KEY_PREFIX = "oauth:mobile:rate-limit:";
    static final String RETRY_AFTER_HEADER = "Retry-After";
    static final String CLIENT_IP_HEADER = "X-Real-IP";
    static final Duration WINDOW = Duration.ofSeconds(60);

    // INCR 와 EXPIRE 를 한 번에 실행해 카운터 키가 TTL 없이 남는 일을 막는다.
    // TTL 이 없는 키(이전 장애 잔재 등)도 여기서 다시 만료를 건다. 반환값: {count, ttl}
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> COUNT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            local ttl = redis.call('TTL', KEYS[1])
            if count == 1 or ttl < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                ttl = tonumber(ARGV[1])
            end
            return {count, ttl}
            """, List.class);

    private final StringRedisTemplate redis;
    private final List<Rule> rules;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(StringRedisTemplate redis, int loginLimitPerMinute, int challengeLimitPerMinute) {
        this.redis = redis;
        Bucket login = new Bucket("login", loginLimitPerMinute);
        Bucket challenge = new Bucket("challenge", challengeLimitPerMinute);
        // SecurityConfig 의 permitAll 과 같은 PathPatternRequestMatcher 를 쓴다.
        // 디코딩된 경로로 비교하므로 /mobile/%6Bakao 같은 인코딩 우회가 통하지 않고, context-path(/api)도 알아서 뗀다.
        this.rules = List.of(
                new Rule(postMatcher(PermitAllUri.OAUTH_MOBILE_KAKAO), login),
                new Rule(postMatcher(PermitAllUri.OAUTH_MOBILE_GOOGLE), login),
                new Rule(postMatcher(PermitAllUri.OAUTH_MOBILE_APPLE), login),
                new Rule(postMatcher(PermitAllUri.OAUTH_MOBILE_APPLE_CHALLENGE), challenge)
        );
    }

    private static RequestMatcher postMatcher(PermitAllUri uri) {
        return PathPatternRequestMatcher.pathPattern(HttpMethod.POST, uri.getUri());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return resolveBucket(request) == null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // shouldNotFilter 가 대상 외 요청을 걸렀으므로 여기서는 항상 버킷이 있다
        Bucket bucket = resolveBucket(request);
        String key = KEY_PREFIX + bucket.name() + ":" + resolveClientIp(request);
        List<?> result;
        try {
            result = redis.execute(COUNT_SCRIPT, List.of(key), String.valueOf(WINDOW.toSeconds()));
        } catch (DataAccessException e) {
            log.error("모바일 로그인 호출 제한 카운터 갱신 실패 (Redis). key={}", key, e);
            writeError(response, GeneralErrorCode.STORE_UNAVAILABLE);
            return;
        }

        if (result == null || result.size() < 2) {
            log.error("모바일 로그인 호출 제한 스크립트 응답이 비정상. key={}, result={}", key, result);
            writeError(response, GeneralErrorCode.STORE_UNAVAILABLE);
            return;
        }

        long count = ((Number) result.get(0)).longValue();
        long ttl = ((Number) result.get(1)).longValue();

        if (count > bucket.limit()) {
            // 창이 끝날 때까지 남은 초. 0이 나와도 클라이언트가 바로 재시도하지 않게 최소 1초
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(Math.max(ttl, 1)));
            writeError(response, OauthErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        for (Rule rule : rules) {
            if (rule.matcher().matches(request)) {
                return rule.bucket();
            }
        }
        return null;
    }

    // nginx 가 X-Real-IP 를 $remote_addr 로 덮어써서 넘기므로 이 헤더만 신뢰한다.
    // X-Forwarded-For 는 클라이언트가 보낸 값이 앞에 남아 위조 가능하므로 쓰지 않는다.
    // 앱 포트(8080)는 외부에 열려 있지 않아 nginx 를 거치지 않은 요청은 들어오지 않는다.
    private String resolveClientIp(HttpServletRequest request) {
        String realIp = request.getHeader(CLIENT_IP_HEADER);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, BaseResponseCode code) throws IOException {
        response.setStatus(code.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.from(code));
    }

    private record Bucket(String name, int limit) {
    }

    private record Rule(RequestMatcher matcher, Bucket bucket) {
    }
}
