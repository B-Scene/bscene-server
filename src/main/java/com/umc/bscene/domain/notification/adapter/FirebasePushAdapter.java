package com.umc.bscene.domain.notification.adapter;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.umc.bscene.domain.notification.dto.response.PushSendResult;
import com.umc.bscene.domain.notification.port.PushPort;

import java.util.HashMap;
import java.util.Map;

public class FirebasePushAdapter implements PushPort {

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushAdapter(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushSendResult send(String targetToken, String title, String body, Map<String, String> data) {
        Map<String, String> payload = new HashMap<>(data);
        payload.put("title", title);
        payload.put("body", body);

        Message message = Message.builder()
                .setToken(targetToken)
                .putAllData(payload)
                .build();

        try {
            firebaseMessaging.send(message);
            return PushSendResult.success();
        } catch (FirebaseMessagingException exception) {
            String errorCode = resolveErrorCode(exception);

            if (MessagingErrorCode.UNREGISTERED.equals(
                    exception.getMessagingErrorCode()
            )) {
                return PushSendResult.invalidToken(
                        errorCode,
                        exception.getMessage()
                );
            }

            return PushSendResult.failed(
                    errorCode,
                    exception.getMessage()
            );
        }
    }

    private String resolveErrorCode(
            FirebaseMessagingException exception
    ) {
        if (exception.getMessagingErrorCode() != null) {
            return exception.getMessagingErrorCode().name();
        }

        if (exception.getErrorCode() != null) {
            return exception.getErrorCode().name();
        }

        return "UNKNOWN";
    }
}
