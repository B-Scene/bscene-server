package com.umc.bscene.domain.notification.adapter;

import com.umc.bscene.domain.notification.dto.response.PushSendResult;
import com.umc.bscene.domain.notification.port.PushPort;

import java.util.Map;

public class NoopPushAdapter implements PushPort {

    @Override
    public PushSendResult send(String targetToken, String title, String body, Map<String, String> data) {
        // FCM 비활성화 상태에서는 실제 푸시를 발송 X
        return PushSendResult.skipped();
    }
}
