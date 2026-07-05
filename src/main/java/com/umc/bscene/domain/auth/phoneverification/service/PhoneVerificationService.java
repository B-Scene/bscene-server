package com.umc.bscene.domain.auth.phoneverification.service;

import com.umc.bscene.domain.auth.phoneverification.dto.request.PhoneVerificationSendRequest;
import com.umc.bscene.domain.auth.phoneverification.dto.request.PhoneVerificationVerifyRequest;
import com.umc.bscene.domain.auth.phoneverification.dto.response.PhoneVerificationSendResponse;
import com.umc.bscene.domain.auth.phoneverification.enums.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.phoneverification.exception.PhoneVerificationException;
import com.umc.bscene.domain.auth.phoneverification.response.code.PhoneVerificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneVerificationService {

    private static final int VERIFICATION_CODE_BOUND = 1_000_000;
    private static final long VERIFICATION_CODE_TTL_SECONDS = 300L;
    private static final long VERIFIED_TTL_SECONDS = 600L;
    private static final String VERIFICATION_KEY_PREFIX = "phoneVerification:";
    private static final String VERIFIED_KEY_PREFIX = "phoneVerification:verified:";

    private static final long SEND_COOLDOWN_SECONDS = 60L;
    private static final String SEND_COOLDOWN_KEY_PREFIX = "phoneVerification:sendCooldown:";

    private static final long DAILY_SEND_LIMIT = 5L;
    private static final long DAILY_SEND_COUNT_TTL_HOURS = 24L;
    private static final String DAILY_SEND_COUNT_KEY_PREFIX = "phoneVerification:sendCount:";

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    private final SmsSender smsSender;

    // 인증번호 발송
    @Transactional
    public PhoneVerificationSendResponse send(PhoneVerificationSendRequest request) {
        validateSendCooldown(request.getPurpose(), request.getPhone());
        validateDailySendLimit(request.getPurpose(), request.getPhone());

        String code = generateVerificationCode();
        String key = generateVerificationKey(request.getPurpose(), request.getPhone());

        smsSender.sendVerificationCode(request.getPhone(), code);

        stringRedisTemplate.opsForValue().set(
                key,
                code,
                Duration.ofSeconds(VERIFICATION_CODE_TTL_SECONDS)
        );

        saveSendCooldown(request.getPurpose(), request.getPhone());
        increaseDailySendCount(request.getPurpose(), request.getPhone());

        return PhoneVerificationSendResponse.builder()
                .expiresInSeconds(VERIFICATION_CODE_TTL_SECONDS)
                .build();
    }

    // 인증번호 인증
    @Transactional
    public void verify(PhoneVerificationVerifyRequest request) {
        String key = generateVerificationKey(request.getPurpose(), request.getPhone());
        String savedCode = stringRedisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new PhoneVerificationException(PhoneVerificationErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }

        if (!savedCode.equals(request.getCode())) {
            throw new PhoneVerificationException(PhoneVerificationErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        stringRedisTemplate.delete(key);

        String verifiedKey = generateVerifiedKey(request.getPurpose(), request.getPhone());
        stringRedisTemplate.opsForValue().set(
                verifiedKey,
                "true",
                Duration.ofSeconds(VERIFIED_TTL_SECONDS)
        );
    }

    public void validateVerified(PhoneVerificationPurpose purpose, String phone) {
        String verifiedKey = generateVerifiedKey(purpose, phone);
        Boolean exists = stringRedisTemplate.hasKey(verifiedKey);

        if (!Boolean.TRUE.equals(exists)) {
            throw new PhoneVerificationException(PhoneVerificationErrorCode.PHONE_VERIFICATION_REQUIRED);
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(VERIFICATION_CODE_BOUND));
    }

    private String generateVerificationKey(PhoneVerificationPurpose purpose, String phone) {
        return VERIFICATION_KEY_PREFIX + purpose.name() + ":" + phone;
    }

    private String generateVerifiedKey(PhoneVerificationPurpose purpose, String phone) {
        return VERIFIED_KEY_PREFIX + purpose.name() + ":" + phone;
    }

    public void removeVerified(PhoneVerificationPurpose purpose, String phone) {
        String verifiedKey = generateVerifiedKey(purpose, phone);
        stringRedisTemplate.delete(verifiedKey);
    }

    private void validateSendCooldown(PhoneVerificationPurpose purpose, String phone) {
        String cooldownKey = generateSendCooldownKey(purpose, phone);
        Boolean exists = stringRedisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(exists)) {
            throw new PhoneVerificationException(PhoneVerificationErrorCode.PHONE_VERIFICATION_SEND_TOO_FREQUENT);
        }
    }

    private void saveSendCooldown(PhoneVerificationPurpose purpose, String phone) {
        String cooldownKey = generateSendCooldownKey(purpose, phone);

        stringRedisTemplate.opsForValue().set(
                cooldownKey,
                "true",
                Duration.ofSeconds(SEND_COOLDOWN_SECONDS)
        );
    }

    private String generateSendCooldownKey(PhoneVerificationPurpose purpose, String phone) {
        return SEND_COOLDOWN_KEY_PREFIX + purpose.name() + ":" + phone;
    }

    private void validateDailySendLimit(PhoneVerificationPurpose purpose, String phone) {
        String dailySendCountKey = generateDailySendCountKey(purpose, phone);
        String count = stringRedisTemplate.opsForValue().get(dailySendCountKey);

        if (count != null && Long.parseLong(count) >= DAILY_SEND_LIMIT) {
            throw new PhoneVerificationException(PhoneVerificationErrorCode.PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED);
        }
    }

    private void increaseDailySendCount(PhoneVerificationPurpose purpose, String phone) {
        String dailySendCountKey = generateDailySendCountKey(purpose, phone);
        Long count = stringRedisTemplate.opsForValue().increment(dailySendCountKey);

        if (count != null && count == 1L) {
            stringRedisTemplate.expire(
                    dailySendCountKey,
                    Duration.ofHours(DAILY_SEND_COUNT_TTL_HOURS)
            );
        }
    }

    private String generateDailySendCountKey(PhoneVerificationPurpose purpose, String phone) {
        return DAILY_SEND_COUNT_KEY_PREFIX + purpose.name() + ":" + phone;
    }
}