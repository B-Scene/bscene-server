package com.umc.bscene.global.security.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// 소셜 로그인 동적 리다이렉트 origin 저장/소비 단위테스트.
// 화이트리스트(CORS 허용 목록) 검증·정규화·일회성 소비·이전 값 승계 방지가 핵심 분기.
class OAuthRedirectOriginSupportTest {

    private OAuthRedirectOriginSupport support;

    @BeforeEach
    void setUp() {
        // 허용 목록에 후행 슬래시가 섞여 있어도 생성 시점에 정규화되는지 함께 검증
        support = new OAuthRedirectOriginSupport(
                new String[]{"https://bscene.app", "http://localhost:5173/"});
    }

    private MockHttpServletRequest requestWithParam(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (origin != null) {
            request.setParameter(OAuthRedirectOriginSupport.PARAM_NAME, origin);
        }
        return request;
    }

    @Test
    void remember_허용_목록의_origin이면_세션에_저장한다() {
        MockHttpServletRequest request = requestWithParam("https://bscene.app");

        support.remember(request);

        assertEquals("https://bscene.app", support.consume(request));
    }

    @Test
    void remember_후행_슬래시는_정규화되어_허용_목록과_비교된다() {
        // 파라미터에 후행 슬래시("https://bscene.app/")가 붙어도 목록의 "https://bscene.app"과 일치
        MockHttpServletRequest request = requestWithParam("https://bscene.app/");

        support.remember(request);

        assertEquals("https://bscene.app", support.consume(request));
    }

    @Test
    void remember_허용_목록의_후행_슬래시도_정규화되어_비교된다() {
        // 목록 쪽에 "http://localhost:5173/"로 등록돼 있어도 정규화된 값으로 매칭
        MockHttpServletRequest request = requestWithParam("http://localhost:5173");

        support.remember(request);

        assertEquals("http://localhost:5173", support.consume(request));
    }

    @Test
    void remember_허용_목록에_없는_origin은_저장하지_않는다() {
        // open redirect 방어 — 공격자가 자기 사이트를 넘겨도 무시되고 기본값이 쓰인다
        MockHttpServletRequest request = requestWithParam("https://evil.example.com");

        support.remember(request);

        assertNull(support.consume(request));
    }

    @Test
    void remember_파라미터가_없으면_저장하지_않는다() {
        MockHttpServletRequest request = requestWithParam(null);

        support.remember(request);

        assertNull(support.consume(request));
    }

    @Test
    void remember_이전_로그인_시도의_값을_먼저_제거한다() {
        // 1차 시도 : 유효한 origin 저장
        MockHttpServletRequest first = requestWithParam("https://bscene.app");
        support.remember(first);

        // 같은 세션으로 2차 시도 : 파라미터 없음 → 1차 값이 승계되면 안 됨
        MockHttpServletRequest second = requestWithParam(null);
        second.setSession(first.getSession());
        support.remember(second);

        assertNull(support.consume(second));
    }

    @Test
    void consume_한_번_꺼내면_세션에서_제거된다() {
        MockHttpServletRequest request = requestWithParam("https://bscene.app");
        support.remember(request);

        assertEquals("https://bscene.app", support.consume(request));
        // 일회성 — 두 번째 소비는 null (핸들러가 기본값 사용)
        assertNull(support.consume(request));
    }

    @Test
    void consume_세션이_아예_없으면_null을_반환한다() {
        // remember를 거치지 않은 요청 (세션 미생성) — 세션을 새로 만들지 않고 null
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(support.consume(request));
    }
}
