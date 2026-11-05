package com.umc.bscene.global.security.oauth.apple;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppleTokenClientTest {

    private static final String CLIENT_ID = "com.bscene.mobile";
    private static final String CLIENT_SECRET = "signed-test-client-secret";
    private static final String SUCCESS_BODY = """
            {"id_token":"apple-id-token","refresh_token":"apple-refresh-token","token_type":"bearer"}
            """;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>(SUCCESS_BODY);
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicInteger bodyDelayMilliseconds = new AtomicInteger();
    private HttpServer server;
    private ExecutorService serverExecutor;
    private AppleClientSecretGenerator secretGenerator;
    private AppleTokenClient tokenClient;

    @BeforeEach
    void setUp() throws Exception {
        secretGenerator = mock(AppleClientSecretGenerator.class);
        when(secretGenerator.generate()).thenReturn(CLIENT_SECRET);
        serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/auth/token", this::respondToTokenRequest);
        server.createContext("/auth/revoke", this::respondToTokenRequest);
        server.start();
        tokenClient = createClient(Duration.ofSeconds(3));
    }

    private void respondToTokenRequest(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        requestMethod.set(exchange.getRequestMethod());
        requestPath.set(exchange.getRequestURI().getPath());
        contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Location", "/must-not-follow");
        exchange.sendResponseHeaders(responseStatus.get(), response.length);
        try (var output = exchange.getResponseBody()) {
            delayResponseBody();
            output.write(response);
        }
    }

    private void delayResponseBody() {
        if (bodyDelayMilliseconds.get() > 0) {
            try {
                Thread.sleep(bodyDelayMilliseconds.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        serverExecutor.shutdownNow();
    }

    @Test
    void exchangesCodeWithEncodedFormAndNoNativeRedirectUri() {
        AppleTokenResponse response = tokenClient.exchange("code+with/&= special");

        assertEquals("apple-id-token", response.identityToken());
        assertEquals("apple-refresh-token", response.refreshToken());
        assertEquals("POST", requestMethod.get());
        assertEquals("application/x-www-form-urlencoded", contentType.get());
        assertEquals(Map.of("grant_type", "authorization_code", "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET, "code", "code+with/&= special"), parseForm(requestBody.get()));
        assertEquals(1, requestCount.get());
        assertFalse(response.toString().contains(response.identityToken()));
        assertFalse(response.toString().contains(response.refreshToken()));
    }

    @Test
    void acceptsCaseInsensitiveBearerTokenType() {
        responseBody.set(SUCCESS_BODY.replace("bearer", "Bearer"));

        assertEquals("apple-id-token", tokenClient.exchange("code").identityToken());
    }

    @Test
    void rejectsBlankAndOversizedCodesWithoutProviderCall() {
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, () -> tokenClient.exchange(null));
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, () -> tokenClient.exchange(" "));
        assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN, () -> tokenClient.exchange("x".repeat(16_385)));
        assertEquals(0, requestCount.get());
    }

    @Test
    void mapsExpiredOrReusedCodeToInvalidProviderTokenWithoutRetry() {
        responseStatus.set(400);
        responseBody.set("{\"error\":\"invalid_grant\",\"error_description\":\"secret-code-value\"}");

        OauthException exception = assertError(OauthErrorCode.INVALID_PROVIDER_TOKEN,
                () -> tokenClient.exchange("secret-code-value"));

        assertNull(exception.getCause());
        assertEquals(1, requestCount.get());
    }

    @Test
    void mapsInvalidClientOrRequestToSanitizedConfigurationFailure() {
        responseStatus.set(400);
        for (String error : new String[]{"invalid_client", "invalid_request", "unauthorized_client"}) {
            responseBody.set("{\"error\":\"" + error + "\",\"error_description\":\"" + CLIENT_SECRET + "\"}");

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> tokenClient.exchange("code"));

            assertNull(exception.getCause());
            assertFalse(exception.getMessage().contains(CLIENT_SECRET));
        }
    }

    @Test
    void mapsProviderRateLimitAndServerFailureToUnavailableWithoutRetry() {
        for (int status : new int[]{429, 500, 503}) {
            responseStatus.set(status);
            responseBody.set("{\"error\":\"temporarily_unavailable\"}");

            assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));
        }
        assertEquals(3, requestCount.get());
    }

    @Test
    void rejectsRedirectWithoutFollowingIt() {
        responseStatus.set(302);

        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));

        assertEquals(1, requestCount.get());
    }

    @Test
    void rejectsUnknownOrMalformedProviderErrors() {
        responseStatus.set(400);
        for (String body : new String[]{"{\"error\":\"unknown\"}", "<html>invalid client</html>", "null", "[]"}) {
            responseBody.set(body);

            assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));
        }
    }

    @Test
    void rejectsMissingOrInvalidSuccessfulResponseFields() {
        for (String body : new String[]{
                "{\"refresh_token\":\"refresh\",\"token_type\":\"bearer\"}",
                "{\"id_token\":\"identity\",\"token_type\":\"bearer\"}",
                "{\"id_token\":\"identity\",\"refresh_token\":\" \" ,\"token_type\":\"bearer\"}",
                "{\"id_token\":123,\"refresh_token\":\"refresh\",\"token_type\":\"bearer\"}",
                "{\"id_token\":\"identity\",\"refresh_token\":\"refresh\",\"token_type\":\"other\"}"}) {
            responseBody.set(body);

            assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));
        }
    }

    @Test
    void rejectsOversizedResponseAndTokenFields() {
        responseBody.set("{\"id_token\":\"" + "x".repeat(16_385)
                + "\",\"refresh_token\":\"refresh\",\"token_type\":\"bearer\"}");
        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));
        responseBody.set("x".repeat(65_537));
        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));
    }

    @Test
    void boundsEntireResponseTimeEvenAfterHeadersArrive() {
        tokenClient = createClient(Duration.ofMillis(250));
        bodyDelayMilliseconds.set(2_000);
        Instant started = Instant.now();

        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.exchange("code"));

        assertTrue(Duration.between(started, Instant.now()).compareTo(Duration.ofSeconds(2)) < 0);
        assertEquals(1, requestCount.get());
    }

    @Test
    void rejectsMissingClientConfigurationBeforeProviderRequest() {
        when(secretGenerator.generate()).thenThrow(new IllegalStateException("Apple 설정이 필요합니다."));

        assertThrows(IllegalStateException.class, () -> tokenClient.exchange("code"));

        assertEquals(0, requestCount.get());
    }

    @Test
    void revokesRefreshTokenWithEncodedFormAndAcceptsEmptySuccess() {
        responseBody.set("");

        tokenClient.revoke("sensitive+refresh/&= token");

        assertEquals("/auth/revoke", requestPath.get());
        assertEquals("POST", requestMethod.get());
        assertEquals("application/x-www-form-urlencoded", contentType.get());
        assertEquals(Map.of("client_id", CLIENT_ID, "client_secret", CLIENT_SECRET,
                "token", "sensitive+refresh/&= token", "token_type_hint", "refresh_token"),
                parseForm(requestBody.get()));
        assertEquals(1, requestCount.get());
    }

    @Test
    void acceptsRepeatedRevocationOnlyWhenAppleReturnsSuccess() {
        responseBody.set("");
        tokenClient.revoke("already-revoked-refresh-token");
        tokenClient.revoke("already-revoked-refresh-token");

        assertEquals(2, requestCount.get());
    }

    @Test
    void retainsFailureForUndocumentedInvalidTokenResponses() {
        responseStatus.set(400);
        for (String error : new String[]{"invalid_token", "invalid_grant"}) {
            responseBody.set("{\"error\":\"" + error + "\",\"error_description\":\"sensitive-refresh\"}");

            assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.revoke("sensitive-refresh"));
        }
        assertEquals(2, requestCount.get());
    }

    @Test
    void rejectsRevocationConfigurationErrorsWithoutExposingCredentials() {
        responseStatus.set(401);
        responseBody.set("{\"error\":\"invalid_client\",\"error_description\":\"" + CLIENT_SECRET + "\"}");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> tokenClient.revoke("sensitive-refresh"));

        assertNull(error.getCause());
        assertFalse(error.getMessage().contains(CLIENT_SECRET));
        assertFalse(error.getMessage().contains("sensitive-refresh"));
    }

    @Test
    void rejectsUnavailableRevocationWithoutAutomaticRetryOrRedirect() {
        responseBody.set("{\"error\":\"temporarily_unavailable\"}");
        for (int status : new int[]{302, 429, 500, 503}) {
            responseStatus.set(status);
            assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.revoke("refresh"));
        }
        assertEquals(4, requestCount.get());
    }

    @Test
    void boundsRevocationResponseTimeAfterHeaders() {
        tokenClient = createClient(Duration.ofMillis(250));
        bodyDelayMilliseconds.set(2_000);
        Instant started = Instant.now();

        assertError(OauthErrorCode.PROVIDER_UNAVAILABLE, () -> tokenClient.revoke("refresh"));

        assertTrue(Duration.between(started, Instant.now()).compareTo(Duration.ofSeconds(2)) < 0);
        assertEquals(1, requestCount.get());
    }

    @Test
    void rejectsMissingOrOversizedRevocationTokensBeforeProviderCall() {
        for (String token : new String[]{null, " ", "x".repeat(16_385)}) {
            assertThrows(IllegalStateException.class, () -> tokenClient.revoke(token));
        }
        assertEquals(0, requestCount.get());
    }

    private AppleTokenClient createClient(Duration timeout) {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/auth/token");
        URI revokeEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/auth/revoke");
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return new AppleTokenClient(CLIENT_ID, secretGenerator, httpClient, endpoint, revokeEndpoint, timeout);
    }

    private Map<String, String> parseForm(String form) {
        return Arrays.stream(form.split("&")).map(field -> field.split("=", 2))
                .collect(Collectors.toMap(field -> decode(field[0]), field -> decode(field[1])));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private OauthException assertError(OauthErrorCode code, Runnable action) {
        OauthException exception = assertThrows(OauthException.class, action::run);
        assertEquals(code, exception.getBaseResponseCode());
        assertNull(exception.getCause());
        return exception;
    }
}
