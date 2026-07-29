package com.umc.bscene.domain.auth.service.verification;

import com.umc.bscene.domain.auth.enums.code.PhoneVerificationErrorCode;
import com.umc.bscene.domain.auth.exception.verification.PhoneVerificationException;
import net.nurigo.sdk.message.exception.NurigoBadRequestException;
import net.nurigo.sdk.message.exception.NurigoInvalidApiKeyException;
import net.nurigo.sdk.message.exception.NurigoUnknownException;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsSenderTest {

    private static final String FROM_NUMBER = "01011112222";
    private static final String TO_NUMBER = "01012345678";
    private static final String CODE = "123456";

    @Mock
    private DefaultMessageService messageService;

    private SmsSender sender;

    @BeforeEach
    void setUp() {
        sender = smsSender("real");
    }

    @Test
    void sendVerificationCode_로그모드이면_외부발송을_호출하지_않는다() {
        SmsSender logSender = smsSender("log");

        assertDoesNotThrow(
                () -> logSender.sendVerificationCode(TO_NUMBER, CODE)
        );

        verifyNoInteractions(messageService);
    }

    @Test
    void sendVerificationCode_정상응답이면_인증문자를_한건_발송한다() {
        SingleMessageSentResponse response =
                org.mockito.Mockito.mock(SingleMessageSentResponse.class);

        when(messageService.sendOne(
                any(SingleMessageSendingRequest.class)
        )).thenReturn(response);

        sender.sendVerificationCode(TO_NUMBER, CODE);

        ArgumentCaptor<SingleMessageSendingRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        SingleMessageSendingRequest.class
                );
        verify(messageService).sendOne(requestCaptor.capture());

        SingleMessageSendingRequest request = requestCaptor.getValue();
        assertThat(request.getMessage().getFrom())
                .isEqualTo(FROM_NUMBER);
        assertThat(request.getMessage().getTo())
                .isEqualTo(TO_NUMBER);
        assertThat(request.getMessage().getText())
                .isEqualTo("[B:Scene] 인증번호는 123456입니다.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("knownSmsExceptions")
    void sendVerificationCode_알려진_외부발송_예외이면_발송실패_예외로_변환한다(
            String description,
            Exception smsException
    ) {
        when(messageService.sendOne(
                any(SingleMessageSendingRequest.class)
        )).thenAnswer(invocation -> {
            throw smsException;
        });

        PhoneVerificationException exception = assertThrows(
                PhoneVerificationException.class,
                () -> sender.sendVerificationCode(TO_NUMBER, CODE)
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(PhoneVerificationErrorCode.SMS_SEND_FAILED);
    }

    @Test
    void sendVerificationCode_예상하지_못한_예외이면_시스템_예외로_전파한다() {
        RuntimeException unexpectedException =
                new RuntimeException("unexpected");

        when(messageService.sendOne(
                any(SingleMessageSendingRequest.class)
        )).thenThrow(unexpectedException);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> sender.sendVerificationCode(TO_NUMBER, CODE)
        );

        assertThat(exception.getMessage())
                .isEqualTo(
                        "CoolSMS 문자 발송 중 예상하지 못한 오류가 발생했습니다."
                );
        assertThat(exception.getCause())
                .isSameAs(unexpectedException);
    }

    private SmsSender smsSender(String mode) {
        SmsSender smsSender = new SmsSender(
                mode,
                "test-api-key",
                "test-api-secret",
                FROM_NUMBER
        );
        ReflectionTestUtils.setField(
                smsSender,
                "messageService",
                messageService
        );
        return smsSender;
    }

    private static Stream<Arguments> knownSmsExceptions() {
        return Stream.of(
                Arguments.of(
                        "잘못된 요청",
                        new NurigoBadRequestException("bad request")
                ),
                Arguments.of(
                        "잘못된 API 키",
                        new NurigoInvalidApiKeyException("invalid api key")
                ),
                Arguments.of(
                        "알 수 없는 API 오류",
                        new NurigoUnknownException("unknown")
                ),
                Arguments.of(
                        "네트워크 통신 오류",
                        new IOException("network error")
                )
        );
    }
}
