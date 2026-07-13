package com.umc.bscene.domain.notification.port;

import com.umc.bscene.domain.notification.dto.response.PushSendResult;

import java.util.Map;

public interface PushPort {

    PushSendResult send(String targetToken, String title, String body, Map<String, String> data);
}
