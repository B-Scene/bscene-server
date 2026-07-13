package com.umc.bscene.domain.notification.adapter;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.umc.bscene.domain.notification.dto.response.PushSendResult;
import com.umc.bscene.domain.notification.port.PushPort;

import java.util.Map;

public class FirebasePushAdapter implements PushPort {

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushAdapter(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushSendResult send(String targetToken, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        try {
            firebaseMessaging.send(message);
            return PushSendResult.success();
        } catch (FirebaseMessagingException exception) {
            if (MessagingErrorCode.UNREGISTERED.equals(
                    exception.getMessagingErrorCode()
            )) {
                return PushSendResult.invalidToken(exception.getMessage());
            }

            return PushSendResult.failed(exception.getMessage());
        }
    }
}
