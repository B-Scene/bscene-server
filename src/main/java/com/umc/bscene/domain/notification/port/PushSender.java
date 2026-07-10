package com.umc.bscene.domain.notification.port;

public interface PushSender {

    void send(String targetToken, String title, String body);
}