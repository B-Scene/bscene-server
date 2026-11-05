package com.umc.bscene.global.security.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    private static final int TOO_MANY_REQUESTS = 429;

    private final RestClient restClient;
    private final String appId;

    public KakaoAccessTokenVerifier(RestClient restClient, String appId) {
        this.restClient = restClient;
        this.appId = appId;
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
        if (appId == null || !appId.matches("\\d+")) {
            throw new IllegalStateException("카카오 모바일 OAuth 앱 ID(KAKAO_APP_ID) 설정이 필요합니다. 카카오 콘솔의 숫자 앱 ID 여야 합니다.");
        }
        if (accessToken == null || accessToken.isBlank()) {
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
            // 응답 본문·예외 메시지에 토큰이 섞일 수 있으므로 상태 코드만 보고 변환한다
            int status = exception.getStatusCode().value();
            if (exception.getStatusCode().is5xxServerError() || status == TOO_MANY_REQUESTS) {
                log.warn("카카오 API 장애 응답. uri={}, status={}", uri, status);
                throw new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
            }
            // 401(무효·만료)을 포함한 4xx 는 토큰 문제로 본다
            throw createInvalidTokenException();
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
        if (!appId.equals(String.valueOf(tokenInfo.appId()))) {
            throw new OauthException(OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
        }
    }

    private void validateUser(KakaoUser user, AccessTokenInfo tokenInfo) {
        // 두 호출 사이에 토큰이 바뀌는 등 사용자 id 가 다르면 신뢰하지 않는다
        if (user.id() == null || !user.id().equals(tokenInfo.id())) {
            throw createInvalidTokenException();
        }
    }

    private static OauthException createInvalidTokenException() {
        return new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
    }

    // ---- 카카오 응답 (필요한 필드만) ----

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
