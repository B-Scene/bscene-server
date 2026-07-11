package com.umc.bscene.domain.notification.adapter;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.umc.bscene.domain.notification.exception.NotificationException;
import com.umc.bscene.domain.notification.port.PushPort;
import com.umc.bscene.domain.notification.response.code.NotificationErrorCode;

import java.util.Map;

public class FirebasePushAdapter implements PushPort {

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushAdapter(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void send(String targetToken, String title, String body, Map<String, String> data) {
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
        } catch (FirebaseMessagingException e) {
            throw new NotificationException(NotificationErrorCode.FCM_SEND_FAILED);
        }
    }
}