package com.umc.bscene.domain.auth.service.verification;

import com.umc.bscene.domain.auth.enums.code.PhoneVerificationErrorCode;
import com.umc.bscene.domain.auth.exception.verification.PhoneVerificationException;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.exception.NurigoBadRequestException;
import net.nurigo.sdk.message.exception.NurigoInvalidApiKeyException;
import net.nurigo.sdk.message.exception.NurigoUnknownException;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class SmsSender {

    private final DefaultMessageService messageService;
    private final String fromNumber;

    private final String mode;

    public SmsSender(
            @Value("${sms.mode}") String mode,
            @Value("${coolsms.api-key}") String apiKey,
            @Value("${coolsms.api-secret}") String apiSecret,
            @Value("${coolsms.from-number}") String fromNumber
    ) {
        this.mode = mode;
        this.messageService = NurigoApp.INSTANCE.initialize(
                apiKey,
                apiSecret,
                "https://api.coolsms.co.kr"
        );
        this.fromNumber = fromNumber;
    }

    public void sendVerificationCode(String phone, String code) {
        if ("log".equalsIgnoreCase(mode)) {
            log.info(
                    "휴대폰 인증번호 테스트 발급 phone={}, code={}",
                    maskPhone(phone),
                    code
            );
            return;
        }

        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(phone);
        message.setText("[B:Scene] 인증번호는 " + code + "입니다.");

        try {
            SingleMessageSentResponse response = messageService.sendOne(
                    new SingleMessageSendingRequest(message)
            );

            log.debug(
                    "CoolSMS 발송 완료: phone={}, statusCode={}, statusMessage={}, messageId={}",
                    maskPhone(phone),
                    response.getStatusCode(),
                    response.getStatusMessage(),
                    response.getMessageId()
            );
        } catch (Exception exception) {
            String maskedPhone = maskPhone(phone);

            // 누리고 SDK가 Java 메소드 시그니처에 구체적인 예외를 선언하지 않으므로
            // 단일 catch에서 실제 예외 타입을 분류한다.
            // 알려진 외부 API 오류는 여기서 원인을 구분해 기록하고,
            // 예상하지 못한 예외만 전역 예외 처리기로 전달한다.
            if (exception instanceof NurigoBadRequestException) {
                log.error(
                        "CoolSMS 요청값 오류: phone={}, message={}",
                        maskedPhone,
                        exception.getMessage(),
                        exception
                );
            } else if (exception instanceof NurigoInvalidApiKeyException) {
                log.error(
                        "CoolSMS API 인증 실패: phone={}, message={}",
                        maskedPhone,
                        exception.getMessage(),
                        exception
                );
            } else if (exception instanceof NurigoUnknownException) {
                log.error(
                        "CoolSMS 알 수 없는 API 오류: phone={}, message={}",
                        maskedPhone,
                        exception.getMessage(),
                        exception
                );
            } else if (exception instanceof IOException) {
                log.warn(
                        "CoolSMS 네트워크 통신 실패: phone={}, message={}",
                        maskedPhone,
                        exception.getMessage(),
                        exception
                );
            } else {
                throw new IllegalStateException(
                        "CoolSMS 문자 발송 중 예상하지 못한 오류가 발생했습니다.",
                        exception
                );
            }

            throw new PhoneVerificationException(
                    PhoneVerificationErrorCode.SMS_SEND_FAILED
            );
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }

        return phone.substring(0, 3)
                + "****"
                + phone.substring(phone.length() - 4);
    }
}