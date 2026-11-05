package com.umc.bscene.global.security.oauth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AppleTokenClient {

    private static final URI TOKEN_ENDPOINT = URI.create("https://appleid.apple.com/auth/token");
    private static final URI REVOKE_ENDPOINT = URI.create("https://appleid.apple.com/auth/revoke");
    private static final int MAX_RESPONSE_BYTES = 65_536;
    private static final Set<String> CONFIGURATION_ERRORS = Set.of(
            "invalid_client", "invalid_request", "unauthorized_client", "unsupported_grant_type", "invalid_scope");

    private final String clientId;
    private final AppleClientSecretGenerator secretGenerator;
    private final HttpClient httpClient;
    private final URI tokenEndpoint;
    private final URI revokeEndpoint;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AppleTokenClient(String clientId, AppleClientSecretGenerator secretGenerator) {
        this(clientId, secretGenerator, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build(), TOKEN_ENDPOINT, Duration.ofSeconds(3));
    }

    AppleTokenClient(String clientId, AppleClientSecretGenerator secretGenerator,
                     HttpClient httpClient, URI tokenEndpoint, Duration requestTimeout) {
        this(clientId, secretGenerator, httpClient, tokenEndpoint, REVOKE_ENDPOINT, requestTimeout);
    }

    AppleTokenClient(String clientId, AppleClientSecretGenerator secretGenerator,
                     HttpClient httpClient, URI tokenEndpoint, URI revokeEndpoint, Duration requestTimeout) {
        this.clientId = clientId;
        this.secretGenerator = secretGenerator;
        this.httpClient = httpClient;
        this.tokenEndpoint = tokenEndpoint;
        this.revokeEndpoint = revokeEndpoint;
        this.requestTimeout = requestTimeout;
    }

    public AppleTokenResponse exchange(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank() || authorizationCode.length() > 16_384) {
            throw new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
        }
        HttpResponse<byte[]> response = send(createRequest(authorizationCode));
        JsonNode body = readResponse(response.body());
        if (response.statusCode() != 200) {
            throwResponseError(response.statusCode(), body);
        }
        return createTokenResponse(body);
    }

    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || refreshToken.length() > 16_384) {
            throw new IllegalStateException("Apple 연결 해제용 자격정보가 올바르지 않습니다.");
        }
        String form = "client_id=" + encode(clientId) + "&client_secret=" + encode(secretGenerator.generate())
                + "&token=" + encode(refreshToken) + "&token_type_hint=refresh_token";
        HttpResponse<byte[]> response = send(createFormRequest(revokeEndpoint, form));
        // Apple은 이미 무효화된 토큰도 200과 빈 본문으로 응답한다.
        if (response.statusCode() != 200) {
            throwRevokeError(response.statusCode(), readResponse(response.body()));
        }
    }

    private HttpRequest createRequest(String authorizationCode) {
        String clientSecret = secretGenerator.generate();
        String form = "grant_type=authorization_code&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret) + "&code=" + encode(authorizationCode);
        return createFormRequest(tokenEndpoint, form);
    }

    private HttpRequest createFormRequest(URI endpoint, String form) {
        return HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
    }

    private void throwRevokeError(int status, JsonNode body) {
        if ((status == 400 || status == 401) && CONFIGURATION_ERRORS.contains(body.path("error").asText(""))) {
            throw new IllegalStateException("Apple OAuth 연결 해제 설정을 확인해 주세요.");
        }
        throw createUnavailableException();
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        CompletableFuture<HttpResponse<byte[]>> pending = httpClient.sendAsync(request,
                ignored -> new LimitedBodySubscriber());
        try {
            // 응답 헤더뿐 아니라 본문 수신까지 제한하며 일회용 code를 자동 재시도하지 않는다.
            return pending.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw createUnavailableException();
        } catch (ExecutionException | TimeoutException exception) {
            throw createUnavailableException();
        } finally {
            pending.cancel(true);
        }
    }

    private JsonNode readResponse(byte[] body) {
        try {
            JsonNode result = objectMapper.readTree(body);
            if (result == null || !result.isObject()) {
                throw createUnavailableException();
            }
            return result;
        } catch (IOException exception) {
            throw createUnavailableException();
        }
    }

    private void throwResponseError(int status, JsonNode body) {
        if (status == 400 || status == 401) {
            String error = body.path("error").asText("");
            if ("invalid_grant".equals(error)) {
                throw new OauthException(OauthErrorCode.INVALID_PROVIDER_TOKEN);
            }
            if (CONFIGURATION_ERRORS.contains(error)) {
                throw new IllegalStateException("Apple OAuth 토큰 교환 설정을 확인해 주세요.");
            }
        }
        throw createUnavailableException();
    }

    private AppleTokenResponse createTokenResponse(JsonNode body) {
        String identityToken = readToken(body, "id_token");
        String refreshToken = readToken(body, "refresh_token");
        if (!body.path("token_type").isTextual() || !"bearer".equalsIgnoreCase(body.path("token_type").textValue())) {
            throw createUnavailableException();
        }
        return new AppleTokenResponse(identityToken, refreshToken);
    }

    private String readToken(JsonNode body, String field) {
        JsonNode token = body.path(field);
        if (!token.isTextual() || token.textValue().isBlank() || token.textValue().length() > 16_384) {
            throw createUnavailableException();
        }
        return token.textValue();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static OauthException createUnavailableException() {
        return new OauthException(OauthErrorCode.PROVIDER_UNAVAILABLE);
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private int receivedBytes;

        @Override
        public CompletionStage<byte[]> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            delegate.onSubscribe(value);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                receivedBytes += buffer.remaining();
                if (receivedBytes > MAX_RESPONSE_BYTES) {
                    subscription.cancel();
                    delegate.onError(new IOException("Apple OAuth 응답 크기가 제한을 초과했습니다."));
                    return;
                }
            }
            delegate.onNext(buffers);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
