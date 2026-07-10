package com.umc.bscene.domain.notification.port;

public interface PushPort {

    void send(String targetToken, String title, String body);
}