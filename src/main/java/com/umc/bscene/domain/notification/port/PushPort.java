package com.umc.bscene.domain.notification.port;

import java.util.Map;

public interface PushPort {

    void send(String targetToken, String title, String body, Map<String, String> data);
}