package com.umc.bscene.domain.auth.service.verification;

import com.umc.bscene.domain.auth.enums.code.PhoneVerificationErrorCode;
import com.umc.bscene.domain.auth.exception.verification.PhoneVerificationException;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
            log.info("휴대폰 인증번호 테스트 발급 phone={}, code={}", phone, code);
            return;
        }

        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(phone);
        message.setText("[B:Scene] 인증번호는 " + code + "입니다.");

        try {
            messageService.sendOne(new SingleMessageSendingRequest(message));
        } catch (Exception e) {
            throw new PhoneVerificationException(PhoneVerificationErrorCode.SMS_SEND_FAILED);
        }
    }
}