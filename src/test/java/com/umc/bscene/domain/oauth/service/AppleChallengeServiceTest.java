package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.response.AppleChallengeResponse;
import com.umc.bscene.domain.oauth.exception.OauthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleChallengeServiceTest {

    private static final String CHALLENGE_ID = "c".repeat(43);
    private static final String NONCE = "n".repeat(43);
    private static final String KEY = "oauth:apple:challenge:" + CHALLENGE_ID;

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    private AppleChallengeService service;

    @BeforeEach
    void setUp() {
        service = new AppleChallengeService(redis);
    }

    @Test
    void issueIndependentRandomValuesWithFiveMinuteExpiry() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(5)))).thenReturn(true);

        AppleChallengeResponse first = service.issue();
        AppleChallengeResponse second = service.issue();

        assertThat(first.challengeId()).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second.challengeId());
        assertThat(first.nonce()).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second.nonce());
        assertThat(first.challengeId()).isNotEqualTo(first.nonce());
        assertThat(first.expiresInSeconds()).isEqualTo(300);
        verify(values).setIfAbsent(
                "oauth:apple:challenge:" + first.challengeId(), first.nonce(), Duration.ofMinutes(5));
        assertThat(first.toString()).doesNotContain(first.challengeId(), first.nonce());
    }

    @Test
    void rejectIssueWhenStoreCannotConfirmInsertion() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertStoreUnavailable(service::issue);
    }

    @Test
    void rejectIssueWhenRedisIsUnavailableWithoutLeakingCause() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("sensitive-redis-address"));
        assertStoreUnavailable(service::issue);
    }

    @Test
    void loadNonceAndRejectMissingOrExpiredChallenge() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(KEY)).thenReturn(NONCE, (String) null);

        assertThat(service.getNonce(CHALLENGE_ID)).isEqualTo(NONCE);
        OauthException error = assertThrows(OauthException.class, () -> service.getNonce(CHALLENGE_ID));
        assertThat(error.getBaseResponseCode().getCode()).isEqualTo("OAUTH400_6");
    }

    @Test
    void rejectLookupWhenRedisFails() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(KEY)).thenThrow(new RedisConnectionFailureException("sensitive"));
        assertStoreUnavailable(() -> service.getNonce(CHALLENGE_ID));
    }

    @Test
    void consumeOnlyWhenAtomicComparisonSucceeds() {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(KEY)), eq(NONCE)))
                .thenReturn(1L, 0L);
        service.consume(CHALLENGE_ID, NONCE);

        OauthException error = assertThrows(OauthException.class, () -> service.consume(CHALLENGE_ID, NONCE));
        assertThat(error.getBaseResponseCode().getCode()).isEqualTo("OAUTH400_6");
    }

    @Test
    void rejectConsumeWhenRedisFailsOrReturnsUnknownOutcome() {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(KEY)), eq(NONCE)))
                .thenThrow(new RedisConnectionFailureException("sensitive"))
                .thenReturn(null);
        assertStoreUnavailable(() -> service.consume(CHALLENGE_ID, NONCE));
        assertStoreUnavailable(() -> service.consume(CHALLENGE_ID, NONCE));
    }

    private void assertStoreUnavailable(Runnable action) {
        OauthException error = assertThrows(OauthException.class, action::run);
        assertThat(error.getBaseResponseCode().getStatus()).isEqualTo(503);
        assertThat(error.getBaseResponseCode().getCode()).isEqualTo("COMMON_503_1");
        assertThat(error.getCause()).isNull();
    }
}
