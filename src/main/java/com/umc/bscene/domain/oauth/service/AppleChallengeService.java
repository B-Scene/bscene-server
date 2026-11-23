package com.umc.bscene.domain.oauth.service;

import com.umc.bscene.domain.oauth.dto.response.AppleChallengeResponse;
import com.umc.bscene.domain.oauth.exception.OauthException;
import com.umc.bscene.domain.oauth.response.code.AppleOauthErrorCode;
import com.umc.bscene.domain.oauth.response.code.OauthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppleChallengeService {

    private static final String KEY_PREFIX = "oauth:apple:challenge:";
    private static final Duration LIFETIME = Duration.ofMinutes(5);
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final SecureRandom secureRandom = new SecureRandom();

    public AppleChallengeResponse issue() {
        String challengeId = generateRandomValue();
        String nonce = generateRandomValue();
        try {
            Boolean saved = redis.opsForValue().setIfAbsent(KEY_PREFIX + challengeId, nonce, LIFETIME);
            if (!Boolean.TRUE.equals(saved)) {
                throw createStoreUnavailableException();
            }
            return new AppleChallengeResponse(challengeId, nonce, LIFETIME.toSeconds());
        } catch (DataAccessException exception) {
            throw createStoreUnavailableException();
        }
    }

    public String getNonce(String challengeId) {
        try {
            String nonce = redis.opsForValue().get(KEY_PREFIX + challengeId);
            if (nonce == null) {
                throw new OauthException(OauthErrorCode.INVALID_APPLE_CHALLENGE);
            }
            return nonce;
        } catch (DataAccessException exception) {
            throw createStoreUnavailableException();
        }
    }

    public void consume(String challengeId, String expectedNonce) {
        try {
            // 검증 중 다른 요청이 소비했거나 값이 바뀌면 코드 교환에 진입하지 않는다.
            Long consumed = redis.execute(CONSUME_SCRIPT, List.of(KEY_PREFIX + challengeId), expectedNonce);
            if (consumed == null) {
                throw createStoreUnavailableException();
            }
            if (consumed != 1L) {
                throw new OauthException(OauthErrorCode.INVALID_APPLE_CHALLENGE);
            }
        } catch (DataAccessException exception) {
            throw createStoreUnavailableException();
        }
    }

    private String generateRandomValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private OauthException createStoreUnavailableException() {
        return new OauthException(AppleOauthErrorCode.AUTH_STORE_UNAVAILABLE);
    }
}
