package com.umc.bscene.domain.notification.adapter;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.umc.bscene.domain.notification.dto.response.PushSendResult;
import com.umc.bscene.domain.notification.enums.PushSendStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebasePushAdapterTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FirebasePushAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FirebasePushAdapter(firebaseMessaging);
    }

    @Test
    void send_알림_필드_없이_제목과_본문을_데이터에_담아_발송한다()
            throws FirebaseMessagingException {
        when(firebaseMessaging.send(any(Message.class)))
                .thenReturn("message-id");

        PushSendResult result = adapter.send(
                "target-token",
                "알림 제목",
                "알림 본문",
                Map.of(
                        "type", "MESSAGE",
                        "deepLink", "/band/session/messages/15"
                )
        );

        ArgumentCaptor<Message> messageCaptor =
                ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(messageCaptor.capture());

        Message message = messageCaptor.getValue();

        assertThat(ReflectionTestUtils.getField(message, "token"))
                .isEqualTo("target-token");
        assertThat(ReflectionTestUtils.getField(message, "notification"))
                .isNull();
        assertThat(messageData(message)).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "title", "알림 제목",
                        "body", "알림 본문",
                        "type", "MESSAGE",
                        "deepLink", "/band/session/messages/15"
                )
        );
        assertThat(result.status()).isEqualTo(PushSendStatus.SUCCESS);
    }

    @Test
    void send_등록이_해제된_토큰이면_무효_토큰_결과를_반환한다()
            throws FirebaseMessagingException {
        FirebaseMessagingException exception =
                mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode())
                .thenReturn(MessagingErrorCode.UNREGISTERED);
        when(exception.getMessage()).thenReturn("token is unregistered");
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        PushSendResult result = adapter.send(
                "invalid-token",
                "제목",
                "본문",
                Map.of()
        );

        assertThat(result.status())
                .isEqualTo(PushSendStatus.INVALID_TOKEN);
        assertThat(result.errorCode()).isEqualTo("UNREGISTERED");
        assertThat(result.errorMessage())
                .isEqualTo("token is unregistered");
    }

    @Test
    void send_FCM_발송_오류이면_실패_결과를_반환한다()
            throws FirebaseMessagingException {
        FirebaseMessagingException exception =
                mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode())
                .thenReturn(MessagingErrorCode.INTERNAL);
        when(exception.getMessage()).thenReturn("internal error");
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        PushSendResult result = adapter.send(
                "target-token",
                "제목",
                "본문",
                Map.of()
        );

        assertThat(result.status()).isEqualTo(PushSendStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("INTERNAL");
        assertThat(result.errorMessage()).isEqualTo("internal error");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> messageData(Message message) {
        return (Map<String, String>) ReflectionTestUtils.getField(
                message,
                "data"
        );
    }
}
