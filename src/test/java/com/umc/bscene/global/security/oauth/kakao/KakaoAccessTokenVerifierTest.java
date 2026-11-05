package com.umc.bscene.global.security.oauth.kakao;

import com.umc.bscene.domain.oauth.enums.SocialProvider;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import com.umc.bscene.domain.oauth.verifier.VerifiedSocialIdentity;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * KakaoAccessTokenVerifier 단위 테스트.
 * <p>
 * 카카오 API 는 MockRestServiceServer 로 대신한다. 기대하지 않은 호출(예: 1차 검증 실패 후 /v2/user/me 호출)은
 * 서버가 AssertionError 를 던지므로 "두 번째 호출을 하지 않는다"는 조건이 자동으로 검증된다.
 */
@DisplayName("KakaoAccessTokenVerifier")
class KakaoAccessTokenVerifierTest {

    private static final String APP_ID = "456789";
    private static final String TOKEN = "kakao-sdk-access-token";
    private static final String TOKEN_INFO_JSON = """
            {"id": 123, "expires_in": 21599, "app_id": 456789}
            """;
    private static final String USER_ME_JSON = """
            {
              "id": 123,
              "connected_at": "2026-11-05T00:00:00Z",
              "kakao_account": {
                "email": "sejin@kakao.com",
                "profile": {"nickname": "세진"}
              }
            }
            """;

    private MockRestServiceServer server;
    private KakaoAccessTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new KakaoAccessTokenVerifier(builder.build(), APP_ID);
    }

    private void expectTokenInfo(ResponseCreator response) {
        server.expect(requestTo(KakaoAccessTokenVerifier.ACCESS_TOKEN_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(response);
    }

    private void expectUserMe(ResponseCreator response) {
        server.expect(requestTo(KakaoAccessTokenVerifier.USER_ME_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(response);
    }

    private static void assertOauthError(ThrowingCallable call, OauthErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(OauthException.class,
                        exception -> assertThat(exception.getBaseResponseCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("정상 검증")
    class Success {

        @Test
        @DisplayName("토큰 정보와 사용자 정보를 차례로 조회해 KAKAO 신원을 만든다")
        void verifiesAndBuildsIdentity() {
            expectTokenInfo(withSuccess(TOKEN_INFO_JSON, MediaType.APPLICATION_JSON));
            expectUserMe(withSuccess(USER_ME_JSON, MediaType.APPLICATION_JSON));

            VerifiedSocialIdentity identity = verifier.verify(TOKEN);

            assertThat(identity.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(identity.providerUid()).isEqualTo("123");
            assertThat(identity.email()).isEqualTo("sejin@kakao.com");
            assertThat(identity.name()).isEqualTo("세진");
            server.verify();
        }

        @Test
        @DisplayName("이메일 미동의·프로필 없음이면 email 과 name 을 null 로 넘긴다 (신규 회원 판단은 공통 처리 몫)")
        void allowsMissingEmailAndProfile() {
            expectTokenInfo(withSuccess(TOKEN_INFO_JSON, MediaType.APPLICATION_JSON));
            expectUserMe(withSuccess("{\"id\": 123, \"kakao_account\": {}}", MediaType.APPLICATION_JSON));

            VerifiedSocialIdentity identity = verifier.verify(TOKEN);

            assertThat(identity.providerUid()).isEqualTo("123");
            assertThat(identity.email()).isNull();
            assertThat(identity.name()).isNull();
        }

        @Test
        @DisplayName("kakao_account 자체가 없어도 예외 없이 null 로 넘긴다")
        void allowsMissingAccount() {
            expectTokenInfo(withSuccess(TOKEN_INFO_JSON, MediaType.APPLICATION_JSON));
            expectUserMe(withSuccess("{\"id\": 123}", MediaType.APPLICATION_JSON));

            VerifiedSocialIdentity identity = verifier.verify(TOKEN);

            assertThat(identity.email()).isNull();
            assertThat(identity.name()).isNull();
        }
    }

    @Nested
    @DisplayName("토큰 거부 → OAUTH401_1")
    class InvalidToken {

        @Test
        @DisplayName("카카오가 401 을 주면 사용자 정보를 조회하지 않고 OAUTH401_1")
        void rejectsUnauthorized() {
            expectTokenInfo(withStatus(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"msg\":\"this access token does not exist\",\"code\":-401}"));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.INVALID_PROVIDER_TOKEN);
            server.verify();
        }

        @Test
        @DisplayName("401 이 아닌 4xx 도 토큰 문제로 본다")
        void rejectsOtherClientErrors() {
            expectTokenInfo(withStatus(HttpStatus.BAD_REQUEST));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.INVALID_PROVIDER_TOKEN);
        }

        @Test
        @DisplayName("두 응답의 사용자 id 가 다르면 OAUTH401_1")
        void rejectsUserIdMismatch() {
            expectTokenInfo(withSuccess(TOKEN_INFO_JSON, MediaType.APPLICATION_JSON));
            expectUserMe(withSuccess("{\"id\": 999}", MediaType.APPLICATION_JSON));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.INVALID_PROVIDER_TOKEN);
        }

        @Test
        @DisplayName("토큰 정보 응답에 id 나 app_id 가 없으면 OAUTH401_1")
        void rejectsIncompleteTokenInfo() {
            expectTokenInfo(withSuccess("{\"expires_in\": 100}", MediaType.APPLICATION_JSON));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.INVALID_PROVIDER_TOKEN);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("빈 토큰은 카카오를 호출하지 않고 OAUTH401_1")
        void rejectsBlankToken(String token) {
            assertOauthError(() -> verifier.verify(token), OauthErrorCode.INVALID_PROVIDER_TOKEN);
            server.verify();
        }

        @Test
        @DisplayName("null 토큰도 OAUTH401_1")
        void rejectsNullToken() {
            assertOauthError(() -> verifier.verify(null), OauthErrorCode.INVALID_PROVIDER_TOKEN);
        }
    }

    @Nested
    @DisplayName("다른 앱 토큰 → OAUTH401_2")
    class AppMismatch {

        @Test
        @DisplayName("app_id 가 비씬 앱이 아니면 사용자 정보를 조회하지 않고 OAUTH401_2")
        void rejectsTokenFromOtherApp() {
            expectTokenInfo(withSuccess("{\"id\": 123, \"app_id\": 111}", MediaType.APPLICATION_JSON));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.PROVIDER_TOKEN_APP_MISMATCH);
            server.verify();
        }
    }

    @Nested
    @DisplayName("카카오 장애 → OAUTH503_1")
    class ProviderUnavailable {

        @Test
        @DisplayName("5xx 면 OAUTH503_1")
        void mapsServerError() {
            expectTokenInfo(withServerError());

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.PROVIDER_UNAVAILABLE);
        }

        @Test
        @DisplayName("429 면 OAUTH503_1 (토큰 문제가 아니라 일시 장애)")
        void mapsTooManyRequests() {
            expectTokenInfo(withStatus(HttpStatus.TOO_MANY_REQUESTS));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.PROVIDER_UNAVAILABLE);
        }

        @Test
        @DisplayName("읽기 타임아웃 등 I/O 실패면 OAUTH503_1")
        void mapsTimeout() {
            expectTokenInfo(withException(new SocketTimeoutException("read timed out")));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.PROVIDER_UNAVAILABLE);
        }

        @Test
        @DisplayName("사용자 정보 호출에서 장애가 나도 OAUTH503_1")
        void mapsFailureOnSecondCall() {
            expectTokenInfo(withSuccess(TOKEN_INFO_JSON, MediaType.APPLICATION_JSON));
            expectUserMe(withServerError());

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.PROVIDER_UNAVAILABLE);
        }

        @Test
        @DisplayName("응답이 JSON 이 아니면 OAUTH503_1")
        void mapsUnparsableBody() {
            expectTokenInfo(withSuccess("<html>maintenance</html>", MediaType.TEXT_HTML));

            assertOauthError(() -> verifier.verify(TOKEN), OauthErrorCode.PROVIDER_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("설정 누락")
    class Misconfiguration {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "not-a-number"})
        @DisplayName("앱 ID 가 비었거나 숫자가 아니면 카카오를 호출하지 않고 IllegalStateException")
        void failsWhenAppIdMissing(String appId) {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer strictServer = MockRestServiceServer.bindTo(builder).build();
            KakaoAccessTokenVerifier misconfigured = new KakaoAccessTokenVerifier(builder.build(), appId);

            assertThatThrownBy(() -> misconfigured.verify(TOKEN))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("KAKAO_APP_ID");
            strictServer.verify();
        }
    }
}
