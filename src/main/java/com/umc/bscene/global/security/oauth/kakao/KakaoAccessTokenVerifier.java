package com.umc.bscene.global.security.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 앱이 카카오 SDK 로 받은 access token 을 서버가 카카오 API 로 검증한다 (명세 SDK v0.3).
 * <p>
 * 1) /v1/user/access_token_info 로 토큰이 유효하고 비씬 카카오 앱(app_id)에서 발급됐는지 확인
 * 2) /v2/user/me 로 사용자 id·이메일·닉네임 조회, 1)의 id 와 같은지 확인
 * <p>
 * 이메일은 선택 동의 항목이라 null 일 수 있다. 신규 회원일 때만 필수인 판단은 OauthService 공통 처리에 맡긴다.
 * access token 원문은 로그·예외 메시지·응답 어디에도 남기지 않는다.
 */
@Slf4j
public final class KakaoAccessTokenVerifier {

    static final String ACCESS_TOKEN_INFO_URI = "https://kapi.kakao.com/v1/user/access_token_info";
    static final String USER_ME_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    // 설정값은 생성 시 한 번만 파싱한다. 비어 있으면 null 로 두고 호출 시점에 실패시킨다
    private final Long appId;

    public KakaoAccessTokenVerifier(RestClient restClient, String appId) {
        this.restClient = restClient;
        this.appId = parseAppId(appId);
    }

    private static Long parseAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        long parsed;
        try {
            parsed = Long.parseLong(appId.trim());
        } catch (NumberFormatException exception) {
            throw createInvalidAppIdException();
        }
        if (parsed <= 0) {
            throw createInvalidAppIdException();
        }
        return parsed;
    }

    // REST API 키나 -1 같은 자리표시자를 넣은 경우. 첫 로그인에서야 500·OAUTH401_2 로 드러나지 않도록
    // 빈 생성 시점에 실패시킨다. 잘못 넣은 값이 키일 수 있으므로 메시지에 값은 남기지 않는다
    private static IllegalStateException createInvalidAppIdException() {
        return new IllegalStateException("카카오 모바일 OAuth 앱 ID(KAKAO_APP_ID) 는 카카오 콘솔의 양의 정수 앱 ID 여야 합니다.");
    }

    public VerifiedSocialIdentity verify(String accessToken) {
        validateRequest(accessToken);

        AccessTokenInfo tokenInfo = fetch(ACCESS_TOKEN_INFO_URI, accessToken, AccessTokenInfo.class);
        validateTokenInfo(tokenInfo);

        KakaoUser user = fetch(USER_ME_URI, accessToken, KakaoUser.class);
        validateUser(user, tokenInfo);

        return new VerifiedSocialIdentity(
                SocialProvider.KAKAO,
                String.valueOf(user.id()),
                user.email(),
                user.nickname()
        );
    }

    private void validateRequest(String accessToken) {
        // 설정 누락은 구글·Apple 검증기와 같이 호출 시점에 실패시킨다 (env 없는 로컬 기동은 막지 않음)
        if (appId == null) {
            throw new IllegalStateException("카카오 모바일 OAuth 앱 ID(KAKAO_APP_ID) 설정이 필요합니다.");
        }
        // 출력 가능한 ASCII 만 허용. 줄바꿈 등이 섞이면 HttpURLConnection 이 헤더 값을 거부하며
        // 토큰이 담긴 IllegalArgumentException 을 던져 500 과 함께 토큰이 로그에 남으므로 여기서 먼저 막는다
        if (accessToken == null || accessToken.isBlank()
                || !accessToken.chars().allMatch(character -> character >= 0x21 && character <= 0x7e)) {
            throw createInvalidTokenException();
        }
    }

    private <T> T fetch(String uri, String accessToken, Class<T> type) {
        try {
            T body = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(type);
            if (body == null) {
                log.warn("카카오 API 응답 본문이 비어 있음. uri={}", uri);
                throw new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
            }
            return body;
        } catch (RestClientResponseException exception) {
            // 예외 메시지에는 응답 본문이 통째로 들어가므로 로그에 남기지 않는다
            HttpStatusCode status = exception.getStatusCode();
            // 이 두 엔드포인트에서 토큰 문제(무효·만료)를 뜻하는 응답은 401(code -401) 뿐이다
            if (HttpStatus.UNAUTHORIZED.isSameCodeAs(status)) {
                throw createInvalidTokenException();
            }
            // 나머지는 전부 카카오 쪽 문제로 본다. 5xx·429 뿐 아니라 내부 오류(-1)·점검(-7)·쿼터 초과(-10)는
            // HTTP 400 으로, 앱 설정 오류(-3, -5)·계정 제재(-4)는 403 으로 온다. 이걸 토큰 문제로 오인하면
            // 장애 동안 모든 사용자가 "토큰 만료" 안내를 받고 재로그인을 반복하므로 503 으로 보내고 원인은 로그로 남긴다
            Integer kakaoCode = status.is4xxClientError() ? readKakaoErrorCode(exception) : null;
            log.warn("카카오 API 오류 응답. uri={}, status={}, code={}", uri, status.value(), kakaoCode);
            throw new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            // 타임아웃, 연결 실패, 본문 파싱 실패. 사용자 데이터는 건드리지 않는다
            log.warn("카카오 API 호출 실패. uri={}, cause={}", uri, exception.getClass().getSimpleName());
            throw new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
        }
    }

    private void validateTokenInfo(AccessTokenInfo tokenInfo) {
        if (tokenInfo.id() == null || tokenInfo.appId() == null) {
            throw createInvalidTokenException();
        }
        // 다른 카카오 앱(다른 서비스)에서 발급한 토큰도 /v1/user/access_token_info 는 200 을 주므로 app_id 로 걸러야 한다
        if (!appId.equals(tokenInfo.appId())) {
            throw new OauthException(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
        }
    }

    private void validateUser(KakaoUser user, AccessTokenInfo tokenInfo) {
        // 두 호출 사이에 토큰이 바뀌는 등 사용자 id 가 다르면 신뢰하지 않는다
        if (user.id() == null || !user.id().equals(tokenInfo.id())) {
            throw createInvalidTokenException();
        }
    }

    // 카카오 오류 본문의 code 는 로그용으로만 읽는다 (본문에 토큰은 없다). 본문이 비었거나 JSON 이 아니면 null.
    // 5xx 의 HTML 장애 페이지까지 파싱할 이유는 없어서 호출부에서 4xx 일 때만 부른다
    private static Integer readKakaoErrorCode(RestClientResponseException exception) {
        try {
            KakaoError error = exception.getResponseBodyAs(KakaoError.class);
            return error == null ? null : error.code();
        } catch (RestClientException | HttpMessageConversionException parseFailure) {
            // 콘텐츠 타입 불일치·IO 는 RestClientException, JSON 파싱 실패는 HttpMessageConversionException
            return null;
        }
    }

    private static OauthException createInvalidTokenException() {
        return new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
    }

    // ---- 카카오 응답 (필요한 필드만) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoError(Integer code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessTokenInfo(Long id, @JsonProperty("app_id") Long appId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoUser(Long id, @JsonProperty("kakao_account") KakaoAccount account) {

        String email() {
            return account == null ? null : account.email();
        }

        String nickname() {
            return account == null || account.profile() == null ? null : account.profile().nickname();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(String email, Profile profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(String nickname) {
    }
}
