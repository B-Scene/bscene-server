package com.umc.bscene.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 소셜 로그인 시작 시 프론트가 redirect_origin 파라미터로 알려준 "돌아갈 프론트 주소"를 세션에 기억해두는 컴포넌트.
 * 배포 프론트와 로컬 프론트가 같은 API 서버로 소셜 로그인할 수 있게 한다.
 * - CORS 허용 목록(cors.allowed-origins)에 있는 origin만 저장 (open redirect 방지 화이트리스트)
 * - 파라미터가 없거나 목록 밖이면 저장하지 않음 → 핸들러가 기본값(FRONT_REDIRECT_ORIGIN)을 사용
 */
@Slf4j
@Component
public class OAuthRedirectOriginSupport {

    public static final String PARAM_NAME = "redirect_origin";
    private static final String SESSION_ATTR = "OAUTH_REDIRECT_ORIGIN";

    private final Set<String> allowedOrigins;

    public OAuthRedirectOriginSupport(@Value("${cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins)
                .map(OAuthRedirectOriginSupport::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    // 로그인 시작 요청의 redirect_origin이 허용 목록에 있으면 세션에 저장
    public void remember(HttpServletRequest request) {
        // 매 로그인 시작마다 이전 시도의 값부터 제거 (파라미터가 없거나 무효면 기본값이 쓰이도록)
        request.getSession().removeAttribute(SESSION_ATTR);

        String origin = request.getParameter(PARAM_NAME);
        if (origin == null || origin.isBlank()) {
            return;
        }

        String normalized = normalize(origin);
        if (allowedOrigins.contains(normalized)) {
            request.getSession().setAttribute(SESSION_ATTR, normalized);
            return;
        }

        // 목록 밖 origin은 무시하고 기본값 사용 — 공격 시도나 프론트 설정 실수(오타 등) 관측용으로 로그만 남김
        log.warn("허용 목록에 없는 redirect_origin 무시 : {}", normalized);
    }

    // 저장해둔 origin을 꺼내고 세션에서 제거 (일회성). 없으면 null → 호출부가 기본값 사용
    public String consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object origin = session.getAttribute(SESSION_ATTR);
        if (origin != null) {
            session.removeAttribute(SESSION_ATTR);
        }
        return (String) origin;
    }

    // 비교가 어긋나지 않게 앞뒤 공백과 후행 슬래시 제거
    private static String normalize(String origin) {
        String trimmed = origin.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
