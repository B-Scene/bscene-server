package com.umc.bscene.domain.auth.service.verification;

import com.umc.bscene.domain.auth.dto.verification.request.PhoneVerificationSendRequest;
import com.umc.bscene.domain.auth.dto.verification.request.PhoneVerificationVerifyRequest;
import com.umc.bscene.domain.auth.dto.verification.response.PhoneVerificationSendResponse;
import com.umc.bscene.domain.auth.enums.code.PhoneVerificationErrorCode;
import com.umc.bscene.domain.auth.enums.verification.PhoneVerificationPurpose;
import com.umc.bscene.domain.auth.exception.verification.PhoneVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    private static final String PHONE = "01012345678";
    private static final PhoneVerificationPurpose PURPOSE =
            PhoneVerificationPurpose.SIGNUP;

    private static final String VERIFICATION_KEY =
            "phoneVerification:SIGNUP:" + PHONE;
    private static final String VERIFIED_KEY =
            "phoneVerification:verified:SIGNUP:" + PHONE;
    private static final String COOLDOWN_KEY =
            "phoneVerification:sendCooldown:SIGNUP:" + PHONE;
    private static final String SEND_COUNT_KEY =
            "phoneVerification:sendCount:SIGNUP:" + PHONE;
    private static final String VERIFY_FAIL_COUNT_KEY =
            "phoneVerification:verifyFailCount:SIGNUP:" + PHONE;
    private static final String VERIFY_COUNT_KEY =
            "phoneVerification:verifyCount:SIGNUP:" + PHONE;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SmsSender smsSender;

    private PhoneVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PhoneVerificationService(
                stringRedisTemplate,
                smsSender
        );
    }

    // ---------- send ----------

    @Test
    void send_재발송_대기시간이_남아있으면_예외() {
        PhoneVerificationSendRequest request = sendRequest();

        when(stringRedisTemplate.hasKey(COOLDOWN_KEY))
                .thenReturn(true);

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.send(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode
                        .PHONE_VERIFICATION_SEND_TOO_FREQUENT
        );
        verifyNoInteractions(smsSender, valueOperations);
    }

    @Test
    void send_일일_발송횟수를_초과하면_예외() {
        PhoneVerificationSendRequest request = sendRequest();

        when(stringRedisTemplate.hasKey(COOLDOWN_KEY))
                .thenReturn(false);
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(SEND_COUNT_KEY))
                .thenReturn("5");

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.send(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode
                        .PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED
        );
        verifyNoInteractions(smsSender);
    }

    @Test
    void send_문자발송이_실패하면_인증정보를_저장하지_않는다() {
        PhoneVerificationSendRequest request = sendRequest();
        PhoneVerificationException smsException =
                new PhoneVerificationException(
                        PhoneVerificationErrorCode.SMS_SEND_FAILED
                );

        when(stringRedisTemplate.hasKey(COOLDOWN_KEY))
                .thenReturn(false);
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(SEND_COUNT_KEY))
                .thenReturn(null);
        doThrow(smsException).when(smsSender)
                .sendVerificationCode(eq(PHONE), anyString());

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.send(request)
        );

        assertThat(exception).isSameAs(smsException);
        verify(valueOperations, never()).set(
                anyString(),
                anyString(),
                any(Duration.class)
        );
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void send_유효한_요청이면_인증번호와_제한정보를_저장한다() {
        PhoneVerificationSendRequest request = sendRequest();

        when(stringRedisTemplate.hasKey(COOLDOWN_KEY))
                .thenReturn(false);
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(SEND_COUNT_KEY))
                .thenReturn(null);
        when(valueOperations.increment(SEND_COUNT_KEY))
                .thenReturn(1L);

        PhoneVerificationSendResponse response = service.send(request);

        assertThat(response.getExpiresInSeconds()).isEqualTo(300L);

        ArgumentCaptor<String> codeCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(smsSender).sendVerificationCode(
                eq(PHONE),
                codeCaptor.capture()
        );

        String verificationCode = codeCaptor.getValue();
        assertThat(verificationCode).matches("\\d{6}");

        verify(valueOperations).set(
                VERIFICATION_KEY,
                verificationCode,
                Duration.ofSeconds(300)
        );
        verify(valueOperations).set(
                COOLDOWN_KEY,
                "true",
                Duration.ofSeconds(60)
        );
        verify(valueOperations).increment(SEND_COUNT_KEY);
        verify(stringRedisTemplate).expire(
                SEND_COUNT_KEY,
                Duration.ofHours(24)
        );
    }

    // ---------- verify ----------

    @Test
    void verify_일일_인증횟수를_초과하면_예외() {
        PhoneVerificationVerifyRequest request =
                verifyRequest("123456");

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(VERIFY_COUNT_KEY))
                .thenReturn("10");

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.verify(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode
                        .PHONE_VERIFICATION_DAILY_VERIFY_LIMIT_EXCEEDED
        );
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void verify_저장된_인증번호가_없으면_예외() {
        PhoneVerificationVerifyRequest request =
                verifyRequest("123456");

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(VERIFY_COUNT_KEY))
                .thenReturn(null);
        when(valueOperations.increment(VERIFY_COUNT_KEY))
                .thenReturn(1L);
        when(valueOperations.get(VERIFICATION_KEY))
                .thenReturn(null);

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.verify(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode.VERIFICATION_CODE_NOT_FOUND
        );
        verify(stringRedisTemplate).expire(
                VERIFY_COUNT_KEY,
                Duration.ofHours(24)
        );
    }

    @Test
    void verify_인증번호가_다르면_실패횟수를_증가시킨다() {
        PhoneVerificationVerifyRequest request =
                verifyRequest("654321");

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(VERIFY_COUNT_KEY))
                .thenReturn(null);
        when(valueOperations.increment(VERIFY_COUNT_KEY))
                .thenReturn(2L);
        when(valueOperations.get(VERIFICATION_KEY))
                .thenReturn("123456");
        when(valueOperations.increment(VERIFY_FAIL_COUNT_KEY))
                .thenReturn(1L);

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.verify(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode.VERIFICATION_CODE_MISMATCH
        );
        verify(stringRedisTemplate).expire(
                VERIFY_FAIL_COUNT_KEY,
                Duration.ofSeconds(300)
        );
        verify(stringRedisTemplate, never()).delete(VERIFICATION_KEY);
    }

    @Test
    void verify_인증번호_실패횟수가_한도에_도달하면_인증정보를_삭제한다() {
        PhoneVerificationVerifyRequest request =
                verifyRequest("654321");

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(VERIFY_COUNT_KEY))
                .thenReturn(null);
        when(valueOperations.increment(VERIFY_COUNT_KEY))
                .thenReturn(2L);
        when(valueOperations.get(VERIFICATION_KEY))
                .thenReturn("123456");
        when(valueOperations.increment(VERIFY_FAIL_COUNT_KEY))
                .thenReturn(5L);

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.verify(request)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode
                        .PHONE_VERIFICATION_VERIFY_LIMIT_EXCEEDED
        );
        verify(stringRedisTemplate).delete(VERIFICATION_KEY);
        verify(stringRedisTemplate).delete(VERIFY_FAIL_COUNT_KEY);
    }

    @Test
    void verify_인증번호가_일치하면_인증완료_정보를_저장한다() {
        PhoneVerificationVerifyRequest request =
                verifyRequest("123456");

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(VERIFY_COUNT_KEY))
                .thenReturn(null);
        when(valueOperations.increment(VERIFY_COUNT_KEY))
                .thenReturn(2L);
        when(valueOperations.get(VERIFICATION_KEY))
                .thenReturn("123456");

        service.verify(request);

        verify(stringRedisTemplate).delete(VERIFICATION_KEY);
        verify(stringRedisTemplate).delete(VERIFY_FAIL_COUNT_KEY);
        verify(valueOperations).set(
                VERIFIED_KEY,
                "true",
                Duration.ofSeconds(600)
        );
    }

    // ---------- verified state ----------

    @Test
    void validateVerified_인증완료_정보가_있으면_통과한다() {
        when(stringRedisTemplate.hasKey(VERIFIED_KEY))
                .thenReturn(true);

        assertDoesNotThrow(
                () -> service.validateVerified(PURPOSE, PHONE)
        );
    }

    @Test
    void validateVerified_인증완료_정보가_없으면_예외() {
        when(stringRedisTemplate.hasKey(VERIFIED_KEY))
                .thenReturn(false);

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> service.validateVerified(PURPOSE, PHONE)
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(
                PhoneVerificationErrorCode.PHONE_VERIFICATION_REQUIRED
        );
    }

    @Test
    void removeVerified_인증완료_정보를_삭제한다() {
        service.removeVerified(PURPOSE, PHONE);

        verify(stringRedisTemplate).delete(VERIFIED_KEY);
    }

    private PhoneVerificationSendRequest sendRequest() {
        PhoneVerificationSendRequest request =
                mock(PhoneVerificationSendRequest.class);
        when(request.getPhone()).thenReturn(PHONE);
        when(request.getPurpose()).thenReturn(PURPOSE);
        return request;
    }

    private PhoneVerificationVerifyRequest verifyRequest(String code) {
        PhoneVerificationVerifyRequest request =
                mock(PhoneVerificationVerifyRequest.class);
        when(request.getPhone()).thenReturn(PHONE);
        lenient().when(request.getCode()).thenReturn(code);
        when(request.getPurpose()).thenReturn(PURPOSE);
        return request;
    }
}
